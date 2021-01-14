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
class MaxPropComparator implements Comparator<Answer>
{
	int me;
	public int compare(Answer a,Answer b)
	{
		return (b.price-a.price)*me;

	}
}
class MySolver extends Solver
{
	int f;
	private static final int MAX = 0x3f3f3f3f;
	private static final int SIZE = 19;
	int myColor;
	int opponentColor;
	public int check(int v)
	{
		if (v<=0)
			return 0;
		switch(v)
		{
			case 2:
				return 5;
			case 3:
				return 15;
			case 5:
				return 100;
			case 6:
				return 400;
			case 8:
				return 300;
			case 9:
				return 850;
			default:
				if(v>=16)
					return 999999;
				else if(v%3==1)
					return 0;
				else if(v%3==2)
					return 600;
				else
					return 1700;
		}
	}
	public int getPrice(int[][] board,int me,int thisColor)
	{
		int [][] L=new int[SIZE+1][SIZE+1];
		int [][] UL=new int[SIZE+1][SIZE+1];
		int [][] U=new int[SIZE+1][SIZE+1];
		int [][] UR=new int[SIZE+1][SIZE+1];
		int v;

		Answer ans=new Answer(null,null,null,0,null);
		for(int i=1;i<=SIZE;i++)
		{
			for(int j=1;j<=SIZE;j++)
			{
				//自己的棋子
				if(board[i][j]==thisColor)
				{
					//System.out.println(i+","+j+","+thisColor);
					v=2;
					//由左到右
					if(j==1)
						L[i][j]=2;
					else
						L[i][j]=L[i][j-1]+3;
					//由左上到右下
					if(i==1||j==1)//左或上
						UL[i][j]=2;
					else
						UL[i][j]=UL[i-1][j-1]+3;
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
				else
				{
					if(board[i][j]==0||board[i][j]==3)
					{
						L[i][j]=UR[i][j]=UL[i][j]=U[i][j]=0;
						v=0;
					}
					else
					{
						L[i][j]=UR[i][j]=UL[i][j]=U[i][j]=-1;
						v=-1;
					}
				}
				if((i==SIZE||v!=thisColor)&&i!=1)
				{
					ans.price+=check(U[i-1][j]+v);
					//System.out.println(ans.price);
				}
				if((j==1||i==SIZE||v!=thisColor)&&i!=1&&j!=SIZE)
				{
					ans.price+=check(UR[i-1][j+1]+v);
					//System.out.println(ans.price);
				}
				if((j==SIZE||i==SIZE||v!=thisColor)&&j!=1&&i!=1)
				{
					ans.price+=check(UL[i-1][j-1]+v);
					//System.out.println(ans.price);
				}
				if((j==SIZE||v!=thisColor)&&j!=1)
				{
					ans.price+=check(L[i][j-1]+v);
					//System.out.println(ans.price);
				}
				//System.out.print(ans.price);
				if(f==1)
					//System.out.print(","+U[i][j]+")");
					//System.out.print(","+UL[i][j]);
					System.out.print(","+UR[i][j]+")");
			}
			if(f==1)
				System.out.println();
		}

		return ans.price;
	}
	//傳入兩個參數color與first
	public String play()
	{
		//取得目前的盤面
		int[][] rootBoard = GetCurrentState();
		myColor = GetMarkTake();
		//目前能夠走的路
		ArrayList<Move>moveHistory = GetAvailableCurrentMoves();

		Answer ans = MinMax(rootBoard,1,1,moveHistory );

		Mark(ans.move1);

		Mark(ans.move2);
		f=1;
		getPrice(rootBoard,1,myColor);
		System.out.println();
		getPrice(rootBoard,1,myColor%2+1);
		f=0;
		return null;
		//return ans.move1.print()+ans.move2.print();
	}
	public Answer MinMax(int[][] board,int deep,int me,ArrayList<Move>moveHistory )
	{
		//找到這個顏色
		int thisColor=0;
		if(me==1)
			thisColor=myColor;
		else
			thisColor=myColor%2+1;
		//能夠找到的所有走法
		ArrayList<Move>moves=moveHistory;

		//定義最初回傳的答案:自己下-無限 別人下+無限
		Answer ans=new Answer(moves.get(0),moves.get(1),board,-me*MAX,moves);

		ArrayList<Answer>answers=new ArrayList<Answer>();
		//下一步搜到的答案
		Answer temp;
		
		for(int i=0;i<moves.size();i++)
			for(int j=i+1;j<moves.size();j++)
			{
				Move move1=moves.get(i);
				Move move2=moves.get(j);
				int[][] tempBoard=GetNextState(board,move1,thisColor);
				int[][] nextBoard=GetNextState(tempBoard,move2,thisColor);
				
				ArrayList<Move>tempMove = GetAvailableMoves(tempBoard,move1,moveHistory);
				ArrayList<Move>nextMove = GetAvailableMoves(nextBoard,move2,tempMove);
				//PrintBoard(nextBoard);

				//if(deep>=1)
				temp=new Answer(null,null,null,0,null);
				temp.price =getPrice(nextBoard,me,myColor)-getPrice(nextBoard,-me,myColor%2+1);
				temp.move1=move1;
				temp.move2=move2;
				temp.board=nextBoard;
				temp.moveHistory=nextMove;
				//else
					//temp = MinMax(nextBoard,deep+1,-me);
				answers.add(temp);
				//Answer a=temp;
				//這手已經贏了
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
		MaxPropComparator ss = new MaxPropComparator();
		//挑選n個目前對自己最有利的下一步
		ss.me=me;
		Collections.sort(answers,ss);
		int longer;
		if(answers.size()<=20)
			longer=answers.size();
		else
			longer=20;
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
			else//對自己最好的一步
				return new Answer(null,null,null,a.price,null);
			//" "+answers.get(i).p());
		}
		Answer a=ans;
		System.out.println("!!"+thisColor+","+a.price+" "+a.move1.print()+a.move2.print());
		return ans;
	}
}
