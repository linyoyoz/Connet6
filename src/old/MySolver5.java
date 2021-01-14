import java.util.*;
import java.lang.*;
import java.io.*;

/*
目前問題針對某一點
針對子點 必須減去母點之四個p[i]，達成米權重平衡

優化方法
1.過濾單一步 price>=99的，如果沒有，過濾 price>=15的
2.針對必勝法 做2子之權重選擇
3.去除所有opt相關的使用

*/
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
	//!!!!!!!!!!!!!!----------------可調整的參數--------------------------
	
	//下第一步時留幾個候選點，給予第二步做深度  ->越高 越慢 越準 每個陣列表示每一層minmax保留的節點數
	int[] FIRST_CHOOSE_SIZE={0,32,32,2,2,1,1};
	//下第二步時留幾個候選點，給予對方做深度  ->越高越慢越準  
	int[] SECOND_CHOOSE_SIZE={0,16,8,2,2,1,1};
	//深度最深到哪一層
	int DEEPTH=6;
	//至少保留多少權重 ->越高越快
	int MUST=95;
	//越高系統會越重視防守?
	int DEF=1;
	
	//-----------------------------------------------------------------
	//example:
	/*  deep=1       rootAns
	               /  |  |  \   <- FIRST_CHOOSE_SIZE=4個最好的
		fs_ans  O   O  O  O
		          /|\ /|\/|\/|\ <-SECOND_CHOOSE_SIZE=3個最好的
		sc_ansOOO OOOOOOOOO
				  |
		deep=2 sc_ans[i]
		        / | | \
				 X X X X         <- FIRST_CHOOSE_SIZE=4個最差的
				/|\
		deep=n  
	*/
	//--------------------------------------------------------------------
	//Debug顯示用
	boolean f;
	boolean DEBUG_KILL=false;
	boolean SPECIAL=false;
	boolean DEBUG_DEEP=false;
	boolean DEBUG_CHOOSE=false;
	//-----------------------
	//定義
	private static final int MAX = 0x3f3f3f3f;
	private static final int SIZE = 19;
	//-----------------------
	int myColor;
	int opponentColor;
	//之後會刪掉，應該不會用到
	int USE_DFS=0;
	
	PrintStream OST;  
	//分四個方向來看
	//左到右、左上到右下、上到下、右上到左下
	
	int [][][] D=new int[4][SIZE+1][SIZE+1];
	int [][][] OLD=new int[4][SIZE+1][SIZE+1];
	//確認是否為起點、終點
	//起點不可以進行price計算、以及dp ，終點必須要進行price 的計算
	//判斷起點cmd帶0
	//判斷終點cmd不帶0
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
	//取得這個座標的起點 
	public Move GetStart(int i,int j,int ch)
	{
			if(ch==0)//左方 ||最左 
				return new Move(i,1);
			else if(ch==1)//左上方 ||最左或最上 
			{
				if(i<j)
					return new Move(1,(j-i)+1);
				else
					return new Move((i-j)+1,1);
			}
			else if(ch==2)//上方
				return new Move(1,j);
			else//右上方
			{
				int x = SIZE+1-j;
				if(i<x)//5,12 -> 1,16 
					return new Move(1,(SIZE+1)-((x-i)+1));
				else//18,2 ->17,3->16,4->1,19 18,4->17,5->16,6->15,7->3,19
					return new Move((i-x)+1,19);
			}
	}
	//確認每個不同的方向的位置與前後位置的關係 ex:ch=0，左向右，其串列為 D[i][n]->D[i][n-1]->D[i][n-2] row 不變 col 為-1 
	public int Ro(int ch)//0 ->0 | 1 ->-1| 2 ->-1| 3 ->-1 
	{
		return ch>0 ?  -1: 0;
	}
	public int Co(int ch)//0 ->-1 | 1 ->-1 | 2 ->0 | 3 ->1
	{
		return ch>0 ? ch-2:-1;
	}
	//判斷兩點有無衝突，也就是 a= O b=X or a=X b=O
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
		return check(NEWV,"+");
		
	}
	//取得此Answer和下點坐標之米字方向的權重
	//每次最多只會跑19*4次
	public int SmartGetPrice(Answer ans,Move m)
	{
		int value=0;
		//四個方向
		for(int i=0;i<4;i++)
		{
			
			//取得方向的起點
			Move mm = GetStart(m.row,m.col,i);
			int r=i,x=mm.row,y=mm.col;
			//取得一列價值
			int v=OneDirectionPrice(ans.board,0,mm,i);
			//將一列之價值儲存
			ans.Set(r,x,y,v);
			if(ans.father!=null)//有父點
			{
				if(SPECIAL)
					OST.printf("--%d:%d-%d=%d\n",i,ans.Get(r,x,y),ans.father.Get(r,x,y),ans.Get(r,x,y)-ans.father.Get(r,x,y));
				value+=ans.Get(r,x,y)-ans.father.Get(r,x,y);
			}
			else//無父點(but應該不會走這裏)
			{
				System.out.println("Warning 根節點使用SmartGetPrice!!");
				value+=ans.Get(r,x,y);
			}
		}

		ans.price+=value;
				
		//根據父點真對整體做調整
		if(SPECIAL)
			OST.printf("! %d+%d=%d\n",value,ans.father.price,ans.price);
		return value;
	}
	//用來計算盤面board的權重 thiscolor 代表這個盤面該下的顏色 ch表示方向
	//計算的核心
	//***如果看到.OOO. 兩邊可以下的(也可以說對方要兩邊檔)稱"活" 每多一顆帶3的倍數(3,6,9...)
	//***如果看到.OX 或 |OO.. 只有一邊可下 稱"死"  每多一顆帶3的倍數-1 (2,5,8...)
	//***如果看到 XOX 或|OX 完全沒有  每多一顆帶3的倍數-2 (1,4,7...)
	//根據算的結果使用check把回傳的ans加總
	
	//每次最多只會跑19次
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
			//if(f)
				//System.out.printf("(%d,%d)",i,j);
			if(board[i][j]==myColor)//電腦的棋子
				me=1;
			else if(board[i][j]==opponentColor)//對方的
				me=-1;
			else//空點
				me=0;
			
			if(!CheckBorder(i,j,ch,0))//非起點，有舊點
			{
				if(me==0)//此點為空
				{
					D[ch][i][j]=0;//帶0
					//確認舊點加總
					price+=checkSite(D[ch][i+Ro(ch)][j+Co(ch)],OLD[ch][i+Ro(ch)][j+Co(ch)]);
			
					//OLD[ch][i+Ro(ch)][j+Co(ch)]= D[ch][i+Ro(ch)][j+Co(ch)];
					//if(count!=25&&USE_DFS==1)
						//break;
				}
				else if(!Conflict(board[i+Ro(ch)][j+Co(ch)],board[i][j]))
					D[ch][i][j]=D[ch][i+Ro(ch)][j+Co(ch)]+3*me;//頭活點，此點與上一點相同或空 OO or .O or XX or .X
				else//此點與上一點不同ex: XXO 對X來講尾死 對O頭死  
				{
					D[ch][i][j]=me*2;//重新計數 ， //新點的頭死點 XO.. or OXX..
					//舊點的尾死點  OOX.  or XXO..
					price+=checkSite(D[ch][i+Ro(ch)][j+Co(ch)]+me,OLD[ch][i+Ro(ch)][j+Co(ch)]);
				}
			}
			else//無舊點(表示i,j為起點)
			{
				if(me==0)
					D[ch][i][j]=0;
				else//新點的頭死點
					D[ch][i][j]=me*2; //頭死點 |O.. or |XX..
			}
			//到終點
			if(CheckBorder(i,j,ch,2))
			{
				if(SPECIAL)
					OST.printf("%d %d enter final\n",i,j);
				price+=checkSite(D[ch][i][j]-me,OLD[ch][i][j]);
				break;
			}
			i-=Ro(ch);
			j-=Co(ch);
		}
		if(SPECIAL)
		{
			OST.println(price);
			printPrice(D[ch],ch);
		}
		count=25;
		i=m.row;
		j=m.col;
		//還原----
		while(count-->0)
		{
			if(CheckBorder(i,j,ch,2))
				break;
			D[ch][i][j]=0;
			i-=Ro(ch);
			j-=Co(ch);
			
		}
		return price;
	}
	public int check(int v,String op)
	{
		int c=1;
		String ss="我";
		if (v==0)
			return 0;
		if(v<0)//表示為對方
		{
			c=-1*DEF;
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
					return c*9999999;
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
	//輸出盤面Debug用
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
		OST.println(s);
		for(int i=1;i<=SIZE;i++)
		{
			for(int j=1;j<=SIZE;j++)
			{
				OST.printf("%2d ",P[i][j]);
			}
			OST.println();
		}
		
	}
	//取得真實盤面的權重
	public int GetCurrentPrice(Answer rootAnswer)
	{
		//確認四個方向的總和
		int [] p=new int[4];
		for(int k=0;k<4;k++)
			p[k]=0;
		//f=true;
		//整個真實盤面做計算		
		for(int i=1;i<=SIZE;i++)
			for(int j=1;j<=SIZE;j++)
				for(int k=0;k<4;k++)		
					if(CheckBorder(i,j,k,0))
					{
						int v=OneDirectionPrice(rootAnswer.board,0,new Move(i,j),k);
						//給予初始點
						rootAnswer.Set(k,i,j,v);
						p[k]+=v;
					}
		int ans=0;
		for(int k=0;k<4;k++)
		{
			ans+=p[k];
			System.out.println(p[k]);
		}
		//總節點初始值給予
		rootAnswer.price=ans;
		return ans;
	}

	public String play()
	{
		//DEBUG過程
		try
		{
			OST = new PrintStream(new FileOutputStream("DEBUG.txt"));        
		}
		catch (FileNotFoundException ex)  
		{
			// insert code to run when exception occurs
		}
		
		
		//取得目前的盤面
		int[][] rootBoard=GetCurrentState();
		
		//取得現在能夠走的步數
		//目前能夠走的路
		ArrayList<Move>moveHistory = GetAvailableCurrentMoves();
		
		Answer rootAnswer=new Answer(null,null,rootBoard,0,moveHistory,null);
		
		//取得自己的顏色
		myColor = GetMarkTake();
		opponentColor=myColor%2+1;
		
		
		//一、--------------確認是否輸了--------------------
		System.out.println("你的");
		int price=GetCurrentPrice(rootAnswer);
		if(price<=-500000)
			return "lost";
				
		
		//二、找必勝法--------------------------------
		int chance=2;
		Answer ans=new Answer(null,null,rootBoard,0,moveHistory,null);
		//針對自己一次，找自己的必勝 kk=0
		//針對敵人兩次，模擬敵人下步是否有必勝法保護自己 kk=1,2
		//-------------DEBUG---------------------
		DEBUG_KILL=false;
		
		if(DEBUG_KILL)
			OST.printf("找必勝法\n");
		
		for(int kk=0;kk<3;kk++)
		{
			int me=-1;
			if(kk==0)
				me=1;
			Answer tempans=Kill(rootAnswer,me);
			if(tempans!=null)
			{
				System.out.printf("find!!!!!!!!!!!!!!! %d %s \n",kk,tempans.move1.print());
				if(kk==0)
				{
					Mark(tempans.move2);
					Mark(tempans.move1);
					chance=0;
					ans.move1=tempans.move1;
					ans.move2=tempans.move2;
					break;
				}
				else
				{
					Mark(tempans.move1);
					chance--;
					if(ans.move1!=null)
						ans.move2=tempans.move1;
					else
						ans.move1=tempans.move1;
				}
				rootAnswer.board=GetCurrentState();
				rootAnswer.moveHistory=GetAvailableCurrentMoves();
			}
			if(DEBUG_KILL)
				OST.printf("--------------%d----------\n",kk);
		}
		DEBUG_KILL=false;
		//三、--------------------------------
		Answer deep_ans=new Answer(null,null,rootBoard,0,moveHistory,null);
		
		//三、深度找點與下點----------------------------		
		if(chance==1)
		{
			deep_ans = MinMax(rootAnswer,1,1,1);
			Mark(deep_ans.move1);
			ans.move2=deep_ans.move1;
		}
		if(chance==2)
		{
			deep_ans = MinMax(rootAnswer,1,1,0);
			Mark(deep_ans.move1);
			Mark(deep_ans.move2);
			ans.move1=deep_ans.move1;
			ans.move2=deep_ans.move2;
		}
		rootBoard=GetCurrentState();
		moveHistory = GetAvailableCurrentMoves();
		rootAnswer=new Answer(null,null,rootBoard,0,moveHistory,null);
		//----------------------------------------------
		//Debug用的
		//------------------------------------------
		//f=true;
		//getPrice(rootBoard,1,myColor);
		//System.out.println();
		//getPrice(rootBoard,1,myColor%2+1);
		//SmartGetPrice(rootAnswer,ans.move1);
		//SmartGetPrice(rootAnswer,ans.move2);
		f=false;
		
		
		//四、確認是否贏了--------------------------
		System.out.println("我的");
		price=GetCurrentPrice(rootAnswer);
		if(price>=500000)
			return "win";
		
		//return sss;
		return ans.move1.print()+ans.move2.print();
	}
	//用來做是否有必勝法的判斷
	public Answer Kill(Answer rootAnswer,int me)
	{
		//找到這個顏色-----------------------
		int thisColor=0;
		if(me==1)//表示電腦下
			thisColor=myColor;
		else//表示對方下
			thisColor=opponentColor;
		//------------------------------------
		
		//能夠找到的所有走法
		ArrayList<Move>moves=rootAnswer.moveHistory;
		int[][] board=rootAnswer.board;
		//下一步搜到的答案
		Answer nextAnswer;
		Answer next2Answer;
		for(int i=0;i<moves.size();i++)
		{
			
			Move move=moves.get(i);//下點i
			//將已經下過的去除
			if(board[move.row][move.col]==1||board[move.row][move.col]==2)
				continue;
			int[][] nextBoard=GetNextState(board,move,thisColor);//找到下個盤面
			ArrayList<Move>nextMove = GetAvailableMoves(nextBoard,move,moves);
			nextAnswer=new Answer(move,null,nextBoard,rootAnswer.price,nextMove,rootAnswer);
			SmartGetPrice(nextAnswer,move);
			
			if((nextAnswer.price>=499999&&me==1)||nextAnswer.price<=-499999&&me==-1)
				return nextAnswer;
			
			for(int j=0;j<nextMove.size();j++)
			{
				Move move2=nextMove.get(j);//下點i
				//將已經下過的去除
				if(board[move2.row][move2.col]==1||board[move2.row][move2.col]==2)
					continue;
				int[][] next2Board=GetNextState(nextBoard,move2,thisColor);
				
				//PrintBoard(next2Board);
				//PrintBoard(next2Board);
				
				//DEBUG==
				//if(move.check(19,11)||move2.check(19,11))
					//SPECIAL=true;
				
				ArrayList<Move>next2Move = GetAvailableMoves(next2Board,move2,nextMove);
				next2Answer=new Answer(move,move2,next2Board,nextAnswer.price,next2Move,nextAnswer);
				
				SmartGetPrice(next2Answer,move2);
				
				//-------------DEBUG---------------------
				if(DEBUG_KILL)
					OST.printf("%d %s %s %d\n",me,move.print(),move2.print(),next2Answer.price);
				
				SPECIAL=false;
				
				//System.out.print(move.print()+",");
				//有輸的
				//OST.printf("%s %s %d\n",move.print(),move2.print(),next2Answer.price);
				if((next2Answer.price>=499999&&me==1)||next2Answer.price<=-499999&&me==-1)
				{
					Answer A_Answer = new Answer(move,move2,GetNextState(board,move,myColor),rootAnswer.price,moves,rootAnswer);
					Answer B_Answer = new Answer(move2,move,GetNextState(board,move2,myColor),rootAnswer.price,moves,rootAnswer);
					
					SmartGetPrice(A_Answer,move);
					SmartGetPrice(B_Answer,move2);
					
					if(A_Answer.price>B_Answer.price)
						return A_Answer;
					else
						return B_Answer;
				}
					//return next2Answer;
				//SmartGetPrice(nextAnswer,move);//取得下個盤面之權重
			}
		}
		return null;
	}
	public Answer MinMax(Answer rootAnswer,int deep,int me,int onlyOne)
	{
		int[][] USE=new int[SIZE][SIZE];
		int[][][][] USE2 = new int[SIZE][SIZE][SIZE][SIZE];
		
		DEBUG_CHOOSE=true;
		if(deep<=1)
		{
			DEBUG_DEEP=true;
			OST.printf("root :%d\n",rootAnswer.price);
		}
		else
			DEBUG_DEEP=false;
	
		int[][] board=rootAnswer.board;
		//找到這個顏色-----------------------
		int thisColor=0;
		if(me==1)//表示電腦下
			thisColor=myColor;
		else//表示對方下
			thisColor=opponentColor;
		//------------------------------------
		//能夠找到的所有走法
		ArrayList<Move>moves=rootAnswer.moveHistory;
		//PrintBoard(board);
		//PrintMoves(moves);
		//定義最初回傳的答案:自己下-無限 別人下+無限
		Answer ans=new Answer(moves.get(0),moves.get(1),board,-me*MAX,moves,rootAnswer);

		ArrayList<Answer>first_answers=new ArrayList<Answer>();
		ArrayList<Answer>second_answers=new ArrayList<Answer>();
		//下一步搜到的答案
		Answer nextAnswer;

		//一、針對所有步數之第一步計算其盤面權重
		//--------------------------------------------------
		for(int i=0;i<moves.size();i++)
		{
			
			Move move=moves.get(i);//下點i
			//將已經下過的去除
			if(board[move.row][move.col]==1||board[move.row][move.col]==2||USE[move.row][move.col]==1)
				continue;
			USE[move.row][move.col]=1;
			//sOST.print(move.print()+",");
			int[][] nextBoard=GetNextState(board,move,thisColor);//找到下個盤面
			ArrayList<Move>nextMove = GetAvailableMoves(nextBoard,move,rootAnswer.moveHistory);
			nextAnswer=new Answer(move,null,nextBoard,rootAnswer.price,nextMove,rootAnswer);
			
			if((move.check(8,8)||move.check(8,9))&&DEBUG_DEEP)
				SPECIAL=true;
		
			SmartGetPrice(nextAnswer,move);//取得下個盤面之權重
			
			SPECIAL=false;
			
			if((nextAnswer.price-rootAnswer.price)*me>=MUST)
			{
				first_answers.add(nextAnswer);
				if(DEBUG_DEEP)
					OST.printf("first:%d %d %d %s\n",deep,nextAnswer.price-rootAnswer.price,first_answers.size(),nextAnswer.move1.print());
			}
		}
		if(DEBUG_DEEP)
			OST.println();
		//二、排序挑選最好的幾步做向下
		//---------------------------------------------------
		MaxPropComparator ss = new MaxPropComparator();
		//挑選n個目前對自己最有利的下一步
		ss.me=me;
		Collections.sort(first_answers,ss);
		int longer;
		
		//抽12個
		if(first_answers.size()<=FIRST_CHOOSE_SIZE[deep])
			longer=first_answers.size();
		else
			longer=FIRST_CHOOSE_SIZE[deep];
		//三、針對12個第一步選所有第二步
		if(DEBUG_DEEP)
			OST.println("~~~");
		int small=MAX;
		for(int i=0;i<longer;i++)
		{
			Answer a= first_answers.get(i);
			//只能下一步時
			if(onlyOne==1)
				if(deep<DEEPTH)
				{
					if(DEBUG_CHOOSE)	
						for(int kk=1;kk<=deep;kk++)
							OST.printf("  ");
					if(DEBUG_CHOOSE)
						OST.println(deep+","+a.price+" "+a.move1.print());
					Answer temp=MinMax(a,deep+1,-me,0);
					
					
					if(DEBUG_CHOOSE&&deep==1)
						OST.println(deep+","+temp.price+" "+a.move1.print());
					
					if(temp.price*me>ans.price*me)
					{
						ans.price=temp.price;
						ans.move1=first_answers.get(i).move1;
						ans.move2=first_answers.get(i).move2;
						
					}
					if(DEBUG_CHOOSE&&deep==1)
						OST.println("------------------------------------------");
				}
				else//到葉子了，對自己最好的一步
				{
					if(DEBUG_CHOOSE)
						OST.println(deep+","+a.price+" "+a.move1.print());
					return a;
				}
			else
				for(int j=0;j<a.moveHistory.size();j++)
				{
					Move move = a.move1;
					//取得第二步
					Move move2=a.moveHistory.get(j);
					//將已經下過的去除
					if(a.board[move2.row][move2.col]==1||a.board[move2.row][move2.col]==2||USE2[move.row][move.col][move2.row][move2.col]==2)
						continue;
					USE2[move.row][move.col][move2.row][move2.col]=2;
					//取得下第二步之盤面
					int[][] nextBoard=GetNextState(a.board,move2,thisColor);
					//取得下第二步之後的行動
					ArrayList<Move>nextMove = GetAvailableMoves(nextBoard,move2,a.moveHistory);
					
					//由a往下繼承其Move1 price 
					nextAnswer=new Answer(a.move1,move2,nextBoard,a.price,nextMove,a);
					SmartGetPrice(nextAnswer,move2);

					if((nextAnswer.price-a.price)*me>=MUST)
					{
						second_answers.add(nextAnswer);
						if(DEBUG_DEEP)
							OST.printf("sc:%d %d %d %s %s\n",deep,nextAnswer.price-a.price,second_answers.size(),nextAnswer.move1.print(),nextAnswer.move2.print());
					}
					//DEBUG
					//OST.printf("%d %s %s %d\n",deep,a.move1.print(),move2.print(),nextAnswer.price);
					//這手已經贏了，如果盤面沒有輸贏，這樣只會有自己下最優或對方下最輸
					if((nextAnswer.price>=499999&&me==1)||nextAnswer.price<=-499999&&me==-1)
						return nextAnswer;
				}
		}
		
		if(onlyOne==1)
			return ans;
		if(DEBUG_DEEP)
			OST.println();
		//-------------------------------------------
		
		//-------------------------------------------
		
	
		//二、排序挑選最好的第二步步
		//挑選n個目前對自己最有利的下一步
		Collections.sort(second_answers,ss);
		//抽5個
		if(second_answers.size()<=SECOND_CHOOSE_SIZE[deep])
			longer=second_answers.size();
		else
			longer=SECOND_CHOOSE_SIZE[deep];
		//針對五個候選做向下深度
		for(int i=0;i<longer;i++)
		{
			Answer ans2= second_answers.get(i);
			if(DEBUG_CHOOSE)	
					for(int kk=1;kk<=deep;kk++)
						OST.printf("  ");
			if(deep<DEEPTH)
			{
				if(DEBUG_CHOOSE)
					OST.println(deep+","+ans2.price+" "+ans2.move1.print()+ans2.move2.print());
				//由這個下一步推算對手可能選的對自己最壞的下一步
				Answer temp=MinMax(ans2,deep+1,-me,0);
				
				if(DEBUG_CHOOSE&&deep==1)
					OST.println(deep+","+temp.price+" "+ans2.move1.print()+ans2.move2.print());
				//OST.println(deep+","+temp.price+" "+ans2.move1.print()+ans2.move2.print());
				//OST.println("~~~~~~");
				//if(deep==2||deep==1)
					
				//me是1時選最大的
				//me是-1時選最小的
				if(temp.price*me>ans.price*me)
				{
					
					ans.price=temp.price;
					ans.move1=second_answers.get(i).move1;
					ans.move2=second_answers.get(i).move2;
					
				}
				if(DEBUG_CHOOSE&&deep==1)
					OST.println("------------------------------------------");
			}
			else//到葉子了，對自己最好的一步
			{
				if(DEBUG_CHOOSE)
					OST.println(deep+","+ans2.price+" "+ans2.move1.print()+ans2.move2.print());
				return ans2;
			}
			//" "+answers.get(i).p());
		}
		//Answer a=ans;
		//System.out.println("!!"+thisColor+","+a.price+" "+a.move1.print()+a.move2.print());
		return ans;
		
	}
}

