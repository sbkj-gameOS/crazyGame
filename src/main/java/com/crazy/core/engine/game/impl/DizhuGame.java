package com.crazy.core.engine.game.impl;

import com.crazy.util.GameUtils;
import com.crazy.util.rules.model.Board;
import com.crazy.util.rules.model.DuZhuBoard;
import com.crazy.util.rules.model.Player;
import com.crazy.web.model.GamePlayway;
import com.crazy.web.model.GameRoom;
import com.crazy.web.model.PlayUserClient;
import com.crazy.core.engine.game.iface.ChessGame;
import org.apache.commons.lang.StringUtils;

import java.util.*;

public class DizhuGame implements ChessGame {
	/**
	 * 开始斗地主游戏
	 * @return
	 */
	public Board process(List<PlayUserClient> playUsers , GameRoom gameRoom , GamePlayway playway ,String banker , int cardsnum){
		Board board = new DuZhuBoard() ;
		board.setCards(null);
		List<Byte> temp = new ArrayList<Byte>() ;
		for(int i= 0 ; i<54 ; i++){
			temp.add((byte)i) ;
		}
		/**
		 * 洗牌次数，参数指定，建议洗牌次数 为1次，多次洗牌的随机效果更好，例如：7次
		 */
		for(int i = 0 ; i<playway.getShuffletimes() ; i++){
			Collections.shuffle(temp);
		}
		byte[] cards = new byte[54] ;
		for(int i=0 ; i<temp.size() ; i++){
			cards[i] = temp.get(i) ;
		}
		board.setCards(cards);
		
		board.setRatio(15); 	//默认倍率 15
		int random = playUsers.size() * gameRoom.getCardsnum() ;
		
		board.setPosition((byte)new Random().nextInt(random));	//按照人数计算在随机界牌 的位置，避免出现在底牌里
		
		Player[] players = new Player[playUsers.size()];
		
		int inx = 0 ;
		for(PlayUserClient playUser : playUsers){
			Player player = new Player(playUser.getId()) ;
			player.setCards(new byte[cardsnum]);
			players[inx++] = player ;
		}
		for(int i = 0 ; i<gameRoom.getCardsnum()*gameRoom.getPlayers(); i++){
			int pos = i%players.length ; 
			players[pos].getCards()[i/players.length] = cards[i] ;
			if(i == board.getPosition()){
				players[pos].setRandomcard(true);		//起到地主牌的人
			}
		}
		for(Player tempPlayer : players){
			Arrays.sort(tempPlayer.getCards());
			tempPlayer.setCards(GameUtils.reverseCards(tempPlayer.getCards()));
		}
		board.setRoom(gameRoom.getId());
		Player tempbanker = players[0];
		if(!StringUtils.isBlank(banker)){
			for(int i= 0 ; i<players.length ; i++){
				Player player = players[i] ;
				if(player.equals(banker)){
					if(i < (players.length - 1)){
						tempbanker = players[i+1] ;
					}
				}
			}
			
		}
		board.setPlayers(players);
		if(tempbanker!=null){
			board.setBanker(tempbanker.getPlayuser());
		}
		return board;
	}

}
