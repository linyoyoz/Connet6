import java.util.*;
import java.lang.*;
//一種用來映射的類別(不一定會用到，先保留)
//紀錄針對某個Board之Price
class Answer
{
	Answer(Move m1,Move m2,int[][] b,int p,ArrayList<Move> move3)
	{
		move1=m1;
		move2=m2;
		price=p;
		board=b;
		moveHistory=move3;
	}
	//Debug顯示用
	String p()
	{
		return "("+move1.print()+","+move2.print()+")";
	}
	Move move1;
	Move move2;
	int price;
	int[][] board;
	//該盤面所有可以走的路
	ArrayList<Move>moveHistory;
}
//排序用
class MaxPropComparator implements Comparator<Answer>
{
	int me;
	//如果是電腦，由大排小
	//如果是對方，由小排大
	public int compare(Answer a,Answer b)
	{
		return (b.price-a.price)*me;

	}
}
class MySolver extends Solver
{
	boolean f;
	private static final int MAX = 0x3f3f3f3f;
	private static final int SIZE = 19;
	int myColor;
	int opponentColor;
	int USE_DFS=0;
	
	//分四個方向來看
	//左到右、左上到右下、上到下、右上到左下
	int [][] L=new int[SIZE+1][SIZE+1];
	int [][] UL=new int[SIZE+1][SIZE+1];
	int [][] U=new int[SIZE+1][SIZE+1];
	int [][] UR=new int[SIZE+1][SIZE+1];
	int [][][] D=new int[4][SIZE+1][SIZE+1];
	int [][][] OLD=new int[4][SIZE+1][SIZE+1];//儲存上一盤面之舊資訊
	//確認是否為起點、終點
	//起點不可以進行price計算、以及dp ，終點必須要進行price 的計算
	//判斷起點cmd帶0
	//判斷終點cmd帶1
	public boolean CheckBorder(int i,int j,int ch,int cmd)
	{
		if(cmd==0)
			cmd=1;
		else
			cmd=SIZE;
		if(j==cmd&&(ch==0||ch==1))//左、左上的起點
			return true;
		if(i==cmd&&(ch==1||ch==2||ch==3))//左上、上、右上的起點
			return true;
		if(j==(SIZE+1)-cmd&&ch==3)//右上的起點
			return true;
		return false;
	}
	public int Ro(int ch)//0 ->0 | 1 ->-1| 2 ->-1| 3 ->-1 
	{
		return ch>0 ?  -1: 0;
	}
	public int Co(int ch)//0 ->-1 | 1 ->-1 | 2 ->0 | 3 ->1
	{
		return ch>0 ? ch-2:-1;
	}
	//判斷兩點有無衝突
	public boolean Conflict(int a,int b)
	{
		if(a==myColor&&b==opponentColor)
			return true;
		else if(a==opponentColor&&b==myColor)
			return true;
		else
			return false;
	}
	public int checkSite(int NEWV,int OLDV)
	{
		int ans=0;
		//這點沒受影響
		if(NEWV==OLDV)
			return 0;
		else
		{
			ans+=check(NEWV,"+");
			ans-=check(OLDV,"-");
		}
		return ans;
	}
	public int SmartGetPrice(int[][] board,Move m)
	{
		int ans=0;
		for(int i=0;i<4;i++)
			ans+=OneDirectionPrice(board,0,m,i);
		return ans;
	}
	//針對某一個方向做判斷
	public int OneDirectionPrice(int[][] board,int price,Move m,int ch)
	{
		int i=m.row,j=m.col;
		int count=25;
		int me;
		if(f)
			System.out.print(m.print());
		while(count-->0)//理論上不會跑超過19次，除非BUG
		{
			if(f)
				System.out.printf("(%d,%d)",i,j);
			if(board[i][j]==myColor)//電腦的棋子
				me=1;
			else if(board[i][j]==opponentColor)//對方的
				me=-1;
			else//空點
				me=0;
			//到終點||me的0且在伸展且Move非此點
			if(CheckBorder(i,j,ch,2))
			{
				//確認自己
				price+=checkSite(D[ch][i][j]-me,OLD[ch][i][j]);
				OLD[ch][i][j]= D[ch][i][j]-me;
				break;
			}
			if(!CheckBorder(i,j,ch,0))//非起點，有舊點
			{
				if(me==0)//此點為空
				{
					D[ch][i][j]=0;//帶0
					//確認舊點加總
					price+=checkSite(D[ch][i+Ro(ch)][j+Co(ch)],OLD[ch][i+Ro(ch)][j+Co(ch)]);
			
					OLD[ch][i+Ro(ch)][j+Co(ch)]= D[ch][i+Ro(ch)][j+Co(ch)];
					if(count!=25&&USE_DFS==1)
						break;
				}
				else if(!Conflict(board[i+Ro(ch)][j+Co(ch)],board[i][j]))//頭活點，此點與上一點相同或空 OO or .O or XX or .X
					D[ch][i][j]=D[ch][i+Ro(ch)][j+Co(ch)]+3*me;
				else//此點與上一點不同 XO 
				{
					D[ch][i][j]=me*2;//重新計數 ， //新點的頭死點 XO.. or OXX..
					//舊點的尾死點  OOX.  or XXO..
					price+=checkSite(D[ch][i+Ro(ch)][j+Co(ch)]+me,OLD[ch][i+Ro(ch)][j+Co(ch)]);
					
					OLD[ch][i+Ro(ch)][j+Co(ch)]= D[ch][i+Ro(ch)][j+Co(ch)]+me;
				}
			}
			else//無舊點
			{
				if(me==0)
					D[ch][i][j]=0;
				else//新點的頭死點
					D[ch][i][j]=me*2; //頭死點 |O.. or |XX..
			}
			i-=Ro(ch);
			j-=Co(ch);
		}
		return price;
	}
	//針對下哪一點進行判斷權重變化
	/*
	public int EasyPrice(int[][] board,int price,Move m,int color)
	{
		//L ->
		int j;
		for(j=m.col;j<=SIZE;j++)
		{
			if(board[m.row][j]==thisColor)//電腦的棋子
				me=1;
			else if(board[m.row][j]==otherColor)//對方的
				me=-1;
			else//空點
				me=0;
			if(j!=1)//非最左邊
			{
				if(me==0)//此點為空
				{
					L[m.row][j]=0;
					check(L[m.row][j-1]);//右活點 OOO..  or XXX..
				}
				else if(!Conflict(board[m.row][j-1],board[m.row][j]))//此點與上一點相同 OO or .O or XX or .X
					L[m.row][j]=L[m.row][j-1]+3*me;
				else//此點與上一點不同 XO 
				{
					L[m.row][j]=me*2;//重新計數
					check(L[m.row][j-1]-me);//右死點  OOX.  or XXO..
				}
			}
			else
			{
				if(me==0)
					L[m.row][j]=0;
				else
					L[m.row][j]=me*2;
			}
			
		}
		price+=check(L[][]);
	}*/
	public int check(int v,String op)
	{
		int c=1;
		String ss="我";
		if (v==0)
			return 0;
		if(v<0)//表示為對方
		{
			c=-1;
			ss="敵";
			v=-v;
		}
		if(f)
		{
			System.out.print(op);
			System.out.print(c*v);
		}
		switch(v)
		{
			//發現一死 ex:..OX..
			case 2:
				if(f)
					System.out.print(ss+"(一死)");
				return c*5;
			case 3://發現一活 ex:..O..
				if(f)
					System.out.print(ss+"(一活)");
				return c*15;
			case 5://發現兩死 ex:..OOX..
				if(f)
					System.out.print(ss+"(兩死)");
				return c*100;
			case 6://發現兩活 ex: ..OO..
				if(f)
					System.out.print(ss+"(兩活)");
				return c*400;
			case 8://發現三死 ex: ..OOOX...
				if(f)
					System.out.print(ss+"(三死)");
				return c*300;
			case 9://發現三活 ex: ...OOO...
				if(f)
					System.out.print(ss+"(三活)");
				return c*850;
			default:
				if(v>=16)//(16,17,18)發現能夠下六個 ex: .XOOOOOOX.
					return c*999999;
				else if(v%3==1)//發現被擋 ex: XOOOOOX
				{
					if(f)
						System.out.print(ss+"(被擋)");
					return 0;
				}
				else if(v%3==2)//(11、14)發現四死、五死 ex:..XOOOO...
				{
					if(f)
						System.out.print(ss+"(一破)");
					return c*600;
				}
				else//(12,15)發現四活、五活 ex:..OOOOO..
				{
					if(f)
						System.out.print(ss+"(兩破)");
					return c*1700;
				}
		}
	}
	public void printPrice(int[][] P,int ch)
	{
		String s;
		switch(ch)
		{
			case 0:
				s="左方";
				break;
			case 1:
				s="左上";
				break;
			case 2:
				s="上方";
				break;
			default:
				s="右上";
		}
		System.out.println(s);
		for(int i=1;i<=SIZE;i++)
		{
			for(int j=1;j<=SIZE;j++)
			{
				System.out.printf("%2d ",P[i][j]);
			}
			System.out.println();
		}
		
	}
	//用來計算盤面board的權重 thiscolor 代表這個盤面該下的顏色 me 可以無視沒用到
	//計算的核心
	//***如果看到.OOO. 兩邊可以下的(也可以說對方要兩邊檔)稱"活" 每多一顆帶3的倍數(3,6,9...)
	//***如果看到.OX 或 |OO.. 只有一邊可下 稱"死"  每多一顆帶3的倍數-1 (2,5,8...)
	//***如果看到 XOX 或|OX 完全沒有  每多一顆帶3的倍數-2 (1,4,7...)
	//根據算的結果使用check把回傳的ans加總
	//便是這個盤面的權重
	public int getPrice(int[][] board,int me,int thisColor)
	{
		
		int v;

		Answer ans=new Answer(null,null,null,0,null);
		for(int i=1;i<=SIZE;i++)
		{
			for(int j=1;j<=SIZE;j++)
			{
				//自己的棋子(顏色為自己)
				if(board[i][j]==thisColor)
				{
					//System.out.println(i+","+j+","+thisColor);
					
					v=thisColor;
					//由左到右
					//------------------------------------------
					if(j==1)//最左邊，一邊死 ex : OO..
						L[i][j]=2;
					else//非最左邊
						L[i][j]=L[i][j-1]+3;//ex :..O=3 , ..OO=6
					//ex:
					//  |..O. = 0030
					//  |OOX. = 25X0
					//  |.OO. = 0360
					//--------------------------------------------------
					
					//2.由左上到右下
					//-------------------------------------------------
					if(i==1||j==1)//左或上
						UL[i][j]=2;
					else
						UL[i][j]=UL[i-1][j-1]+3;
					
					//ex:
					//  ----
					//  .OOO  = 0222 
					//  .OOX    035X
					//  XOOO    X368
					//--------------------------------------------------
					//由上到下
					if(i==1)//上
						U[i][j]=2;
					else
						U[i][j]=U[i-1][j]+3;
					//由右上到左下
					if(i==1||j==SIZE)
						UR[i][j]=2;
					else
						UR[i][j]=UR[i-1][j+1]+3;
				}
				else//這個位置下的棋子非自己
				{
					//空點
					if(board[i][j]==0||board[i][j]==3)
					{
						L[i][j]=UR[i][j]=UL[i][j]=U[i][j]=0;
						v=0;
					}
					else//對方的棋子會擋住自己因此-1
					{
						L[i][j]=UR[i][j]=UL[i][j]=U[i][j]=-1;
						v=-1;
					}
				}
				//上到下做ans加總
				//如果到最下面(i==SIZE)或是此點非自己的點
				if((i==SIZE||v!=thisColor)&&i!=1)
				{
					ans.price+=check(U[i-1][j]+v,"-");
					//System.out.println(ans.price);
				}
				//右上到左下
				//如果到最下面(i==SIZE)或最左面(j==1)或是此點非自己的點
				if((j==1||i==SIZE||v!=thisColor)&&i!=1&&j!=SIZE)
				{
					ans.price+=check(UR[i-1][j+1]+v,"-");
					//System.out.println(ans.price);
				}
				if((j==SIZE||i==SIZE||v!=thisColor)&&j!=1&&i!=1)
				{
					ans.price+=check(UL[i-1][j-1]+v,"-");
					//System.out.println(ans.price);
				}
				if((j==SIZE||v!=thisColor)&&j!=1)
				{
					ans.price+=check(L[i][j-1]+v,"-");
					//System.out.println(ans.price);
				}
				
			}
		}
		//debug 輸出
		//if(f)
		//{
			//printPrice(U,"上方");
			//printPrice(UL,"左上");
			//printPrice(UR,"右上");
			//printPrice(L,"左方");
		//}
		return ans.price;
	}
	//目前問題，需要能夠取得對方下的點
	public String play()
	{
		int [] p=new int[4];
		//取得目前的盤面
		int[][] rootBoard=GetCurrentState();
		//取得自己的顏色
		myColor = GetMarkTake();
		opponentColor=myColor%2+1;
		//取得現在能夠走的步數
		//目前能夠走的路
		ArrayList<Move>moveHistory = GetAvailableCurrentMoves();
		
		USE_DFS=0;
		
		//整個真實盤面做計算
		for(int i=1;i<=SIZE;i++)
			for(int j=1;j<=SIZE;j++)
				for(int k=0;k<4;k++)
					OLD[k][i][j]=0;
				
		for(int i=1;i<=SIZE;i++)
			for(int j=1;j<=SIZE;j++)
				for(int k=0;k<4;k++)		
					if(CheckBorder(i,j,k,0))
						p[k]=OneDirectionPrice(rootBoard,p[k],new Move(i,j),k);
				
		USE_DFS=1;
		/*
		int[][] tempBoard=GetNextState(rootBoard,moves.get(0),myColor);
		int[][] nextBoard=GetNextState(tempBoard,moves.get(1),myColor);
		PrintBoard(GetCurrentState());
		PrintBoard(nextBoard);
		*/
		
		Answer ans = MinMax(rootBoard,1,1,moveHistory);
		
		
		Mark(ans.move1);

		Mark(ans.move2);
		//Debug用的
		//-----------------
		f=true;
		//getPrice(rootBoard,1,myColor);
		//System.out.println();
		//getPrice(rootBoard,1,myColor%2+1);
		for(int i=0;i<4;i++)
		{
			System.out.println(p[i]);
			p[i]=OneDirectionPrice(rootBoard,p[i],ans.move1,i);
			System.out.println(p[i]);
			p[i]=OneDirectionPrice(rootBoard,p[i],ans.move2,i);
			System.out.println(p[i]);
			printPrice(D[i],i);
		}
		f=false;
		//-----------------
		return null;
		//return ans.move1.print()+ans.move2.print();
	}
	public Answer MinMax(int[][] board,int deep,int me,ArrayList<Move>moveHistory)
	{
		//找到這個顏色
		int thisColor=0;
		if(me==1)//表示電腦下
			thisColor=myColor;
		else//表示對方下
			thisColor=opponentColor;
		//能夠找到的所有走法
		ArrayList<Move>moves=moveHistory;

		//定義最初回傳的答案:自己下-無限 別人下+無限
		Answer ans=new Answer(moves.get(0),moves.get(1),board,-me*MAX,moves);

		ArrayList<Answer>answers=new ArrayList<Answer>();
		//下一步搜到的答案
		Answer temp;

		//一、針對所有步數計算其盤面權重
		//--------------------------------------------------
		for(int i=0;i<moves.size();i++)
			for(int j=i+1;j<moves.size();j++)
			{
				Move move1=moves.get(i);
				Move move2=moves.get(j);
				int[][] tempBoard=GetNextState(board,move1,thisColor);
				int[][] nextBoard=GetNextState(tempBoard,move2,thisColor);

				ArrayList<Move>tempMove = GetAvailableMoves(tempBoard,move1,moveHistory);
				SmartGetPrice(nextBoard,move1);
				ArrayList<Move>nextMove = GetAvailableMoves(nextBoard,move2,tempMove);
				//PrintBoard(nextBoard);

				//if(deep>=1)
				temp=new Answer(null,null,null,0,null);
				
				//電腦贏-對方贏
				//***這裡用很笨的方法 算兩次整個盤面再相減，之後可再改良
				temp.price =SmartGetPrice(nextBoard,move2);
				temp.move1=move1;
				temp.move2=move2;
				temp.board=nextBoard;
				temp.moveHistory=nextMove;
				
				//else
					//temp = MinMax(nextBoard,deep+1,-me);
				answers.add(temp);
				
				//還原-----------------------------
				nextBoard[move1.row][move1.col]=0;
				nextBoard[move2.row][move2.col]=0;
				SmartGetPrice(nextBoard,move1);
				SmartGetPrice(nextBoard,move2);
				//---------------------------------
				
				//Answer a=temp;

				//這手已經贏了，如果盤面沒有輸贏，這樣只會有自己下最優或對方下最輸
				if(temp.price>=499999||temp.price<=-499999)
					return temp;
				//System.out.print(move1.print()+move2.print());
				//System.out.println(temp.price);
				//自己下取最大，別人下取最小
				/*
				if(temp.price*me>ans.price*me)
				{
					ans.price=temp.price;
					ans.move1=move1;
					ans.move2=move2;
				}*/
			}
		//-------------------------------------------
		//二、排序挑選最好的幾步做向下
		//-------------------------------------------
		MaxPropComparator ss = new MaxPropComparator();
		//挑選n個目前對自己最有利的下一步
		ss.me=me;
		Collections.sort(answers,ss);
		int longer;
		
		//抽5個
		if(deep==1)
			longer=answers.size();
		else if(answers.size()<=5)
			longer=answers.size();
		else
			longer=5;
		
		for(int i=0;i<longer;i++)
		{
			Answer a= answers.get(i);
			if(deep<=2)
			{
				//由這個下一步推算對手可能選的對自己最壞的下一步
				temp=MinMax(answers.get(i).board,deep+1,-me,answers.get(i).moveHistory);
				if(deep==2||deep==1)
					System.out.println(thisColor+","+temp.price+" "+a.move1.print()+a.move2.print());
				//me是1時選最大的
				//me是-1時選最小的
				if(temp.price*me>ans.price*me)
				{
					ans.price=temp.price;
					ans.move1=answers.get(i).move1;
					ans.move2=answers.get(i).move2;
				}
			}
			else//到葉子了，對自己最好的一步
				return new Answer(null,null,null,a.price,null);
			//" "+answers.get(i).p());
		}
		Answer a=ans;
		System.out.println("!!"+thisColor+","+a.price+" "+a.move1.print()+a.move2.print());
		return ans;
	}
}

/*
目前的想法
設OLD全0
先對真實盤面找全部(4*19*19)
並找到一個權重ans 

某點改動 只會影響米字4*19
if OLD(舊)!=D(舊)
	啟用補償
	+D -OLD
else 一樣
	不做任何事
	
*/
