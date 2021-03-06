package com.crazy.util.rules.model;

import com.crazy.core.BMDataContext;
import com.crazy.core.engine.game.ActionTaskUtils;
import com.crazy.core.engine.game.BeiMiGameEvent;
import com.crazy.core.engine.game.CardType;
import com.crazy.web.model.GameRoom;
import com.crazy.web.model.PlayUserClient;
import com.crazy.util.GameUtils;
import com.crazy.util.cache.CacheHelper;
import org.apache.commons.lang.ArrayUtils;

/**
 * 牌局，用于描述当前牌局的内容 ， 
 * 1、随机排序生成的 当前 待起牌（麻将、德州有/斗地主无）
 * 2、玩家 手牌
 * 3、玩家信息
 * 4、当前牌
 * 5、当前玩家
 * 6、房间/牌桌信息
 * 7、其他附加信息
 * 数据结构内存占用 78 byte ， 一副牌序列化到 数据库 占用的存储空间约为 78 byt， 数据库字段长度约为 20
 *
 * @author iceworld
 *
 */
public class DuZhuBoard extends Board implements java.io.Serializable{

	/**
	 *
	 */
	private static final long serialVersionUID = 6143646772231515350L;

	/**
	 * 翻底牌 ， 斗地主
	 */
	@Override
	public byte[] pollLastHands() {
		return ArrayUtils.subarray(this.getCards() , this.getCards().length - 3 , this.getCards() .length);
	}

	/**
	 * 暂时不做处理，根据业务规则修改，例如：底牌有大王翻两倍，底牌有小王 翻一倍，底牌是顺子 翻两倍 ====
	 */
	@Override
	public int calcRatio() {
		return 1;
	}

	@Override
	public TakeCards takeCards(Player player , String playerType, TakeCards current) {
		return new TakeDiZhuCards(player);
	}


	/**
	 * 找到玩家
	 * @param board
	 * @param userid
	 * @return
	 */
	public Player player(String userid){
		Player target = null ;
		for(Player temp : this.getPlayers()){
			if(temp.getPlayuser().equals(userid)){
				target = temp ; break ;
			}
		}
		return target ;
	}

	/**
	 * 找到玩家的 位置
	 * @param board
	 * @param userid
	 * @return
	 */
	public int index(String userid){
		int index = 0;
		for(int i=0 ; i<this.getPlayers().length ; i++){
			Player temp = this.getPlayers()[i] ;
			if(temp.getPlayuser().equals(userid)){
				index = i ; break ;
			}
		}
		return index ;
	}


	/**
	 * 找到下一个玩家
	 * @param board
	 * @param index
	 * @return
	 */
	public Player next(int index){
		Player catchPlayer = null;
		if(index == 0 && this.getPlayers()[index].isRandomcard()){	//fixed
			index = this.getPlayers().length - 1 ;
		}
		for(int i = index ; i>=0 ; i--){
			Player player = this.getPlayers()[i] ;
			if(player.isDocatch() == false){
				catchPlayer = player ;
				break ;
			}else if(player.isRandomcard()){	//重新遍历一遍，发现找到了地主牌的人，终止查找
				break ;
			}else if(i == 0){
				i = this.getPlayers().length;
			}
		}
		return catchPlayer;
	}


	public Player nextPlayer(int index) {
		if(index == 0){
			index = this.getPlayers().length - 1 ;
		}else{
			index = index - 1 ;
		}
		return this.getPlayers()[index];
	}
	/**
	 *
	 * @param player
	 * @param current
	 * @return
	 */
	public TakeCards takecard(Player player , boolean allow , byte[] playCards) {
		return new TakeDiZhuCards(player , allow , playCards);
	}

	/**
	 * 当前玩家随机出牌，能管住当前出牌的 最小牌
	 * @param player
	 * @param current
	 * @return
	 */
	public TakeCards takecard(Player player) {
		return new TakeDiZhuCards(player);
	}

	/**
	 * 当前玩家随机出牌，能管住当前出牌的 最小牌
	 * @param player
	 * @param current
	 * @return
	 */
	public TakeCards takecard(Player player , TakeCards last) {
		return new TakeDiZhuCards(player, last);
	}

	@Override
	public boolean isWin() {
		boolean win = false ;
		if(this.getLast()!=null && this.getLast().getCardsnum() == 0){//出完了
			win = true ;
		}
		return win;
	}

	@Override
	public TakeCards takeCardsRequest(GameRoom gameRoom , Board board, Player player,
                                      String orgi, boolean auto, byte[] playCards) {
		TakeCards takeCards = null ;
		//超时了 ， 执行自动出牌
		if(auto == true || playCards != null){
			if(board.getLast() == null || board.getLast().getUserid().equals(player.getPlayuser())){	//当前无出牌信息，刚开始出牌，或者出牌无玩家 压
				/**
				 * 超时处理，如果当前是托管的或玩家超时，直接从最小的牌开始出，如果是 AI，则 需要根据AI级别（低级/中级/高级） 计算出牌 ， 目前先不管，直接从最小的牌开始出
				 */
				takeCards = board.takecard(player , true , playCards) ;
			}else{
				if(playCards == null){
					takeCards = board.takecard(player , board.getLast()) ;
				}else{
					CardType playCardType = ActionTaskUtils.identification(playCards) ;
					CardType lastCardType = ActionTaskUtils.identification(board.getLast().getCards()) ;
					if(ActionTaskUtils.allow(playCardType, lastCardType)){//合规，允许出牌
						takeCards = board.takecard(player , true , playCards) ;
					}else{
						//不合规的牌 ， 需要通知客户端 出牌不符合规则 ， 此处放在服务端判断，防外挂
					}
				}
			}
		}else{
			takeCards = new TakeDiZhuCards();
			takeCards.setUserid(player.getPlayuser());
		}
		if(takeCards!=null){		//通知出牌
			takeCards.setCardsnum(player.getCards().length);
			takeCards.setAllow(true);

			if(takeCards.getCards()!=null){
				board.setLast(takeCards);
				takeCards.setDonot(false);	//出牌
			}else{
				takeCards.setDonot(true);	//不出牌
			}
			Player next = board.nextPlayer(board.index(player.getPlayuser())) ;
			if(next!=null){
				takeCards.setNextplayer(next.getPlayuser());
				board.setNextplayer(next.getPlayuser());

			}
			CacheHelper.getBoardCacheBean().put(gameRoom.getId(), board, gameRoom.getOrgi());
			/**
			 * 判断下当前玩家是不是和最后一手牌 是一伙的，如果是一伙的，手机端提示 就是 不要， 如果不是一伙的，就提示要不起
			 */
			if(player.getPlayuser().equals(board.getBanker())){ //当前玩家是地主
				takeCards.setSameside(false);
			}else{
				if(board.getLast().getUserid().equals(board.getBanker())){ //最后一把是地主出的，然而我却不是地主
					takeCards.setSameside(false);
				}else{
					takeCards.setSameside(true);
				}
			}
			/**
			 * 移除定时器，然后重新设置
			 */
			CacheHelper.getExpireCache().remove(gameRoom.getRoomid());


			if(takeCards.getCards().length == 1){
				takeCards.setCard(takeCards.getCards()[0]);
			}
			ActionTaskUtils.sendEvent("takecards", takeCards , gameRoom);

			/**
			 * 牌出完了就算赢了
			 */
			if(board.isWin()){//出完了
				GameUtils.getGame(gameRoom.getPlayway() , orgi).change(gameRoom , BeiMiGameEvent.ALLCARDS.toString() , 0);	//赢了，通知结算
				takeCards.setNextplayer(null);
			}else{
				PlayUserClient nextPlayUserClient = ActionTaskUtils.getPlayUserClient(gameRoom.getId(), takeCards.getNextplayer(), orgi) ;
				if(BMDataContext.PlayerTypeEnum.NORMAL.toString().equals(nextPlayUserClient.getPlayertype())){
					GameUtils.getGame(gameRoom.getPlayway() , orgi).change(gameRoom , BeiMiGameEvent.PLAYCARDS.toString() , 25);	//应该从 游戏后台配置参数中获取
				}else{
					GameUtils.getGame(gameRoom.getPlayway() , orgi).change(gameRoom , BeiMiGameEvent.PLAYCARDS.toString() , 3);	//应该从游戏后台配置参数中获取
				}
			}
		}else{
			takeCards = new TakeDiZhuCards();
			takeCards.setAllow(false);
		}
		return takeCards;
	}

	@Override
	public void dealRequest(GameRoom gameRoom, Board board, String orgi , boolean reverse, String nextplayer) {
		/**
		 * 斗地主无发牌动作
		 */
	}

	@Override
	public void playcards(Board board, GameRoom gameRoom, Player player,
                          String orgi) {
	}
	
}
