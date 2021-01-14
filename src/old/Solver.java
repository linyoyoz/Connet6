import java.util.*;

public class Solver {

	private static int[][] BoardNow = new int[20][20];
	private static ArrayList<Move>MovesNow = new ArrayList<Move>();
	private int myTake = 0;
	//給出目前真實盤面的可走位置
	public ArrayList<Move> GetAvailableCurrentMoves(){
		
		return MovesNow;
		
	}

	public static ArrayList<Move> GetAvailableMoves(int[][] Board,Move move,ArrayList<Move> beforeMove1) {
    		
    		ArrayList<Move> beforeMove=new ArrayList<Move>(beforeMove1);
    		
    	for(int i=0;i<beforeMove.size();i++)
		{
    		if(Board[beforeMove.get(i).row][beforeMove.get(i).col]==Board[move.row][move.col]) 
    			beforeMove.remove(i);
    	}
		
    			if(Board[move.row][move.col-1]==0)
    				
    				beforeMove.add(new Move(move.row,move.col-1));
    			
    			if(Board[move.row][move.col-2]==0)
    				
        			beforeMove.add(new Move(move.row,move.col-2));
    			if(Board[move.row][move.col+1]==0)
    				
        			beforeMove.add(new Move(move.row,move.col+1));
        			
        		if(Board[move.row][move.col+2]==0)
        				
            		beforeMove.add(new Move(move.row,move.col+2));
        		
        		if(Board[move.row-1][move.col]==0)
    				
        			beforeMove.add(new Move(move.row-1,move.col));
        		
        		if(Board[move.row-1][move.col-1]==0)
    				
        			beforeMove.add(new Move(move.row-1,move.col-1));
        			
        		if(Board[move.row-1][move.col-2]==0)
        				
            		beforeMove.add(new Move(move.row-1,move.col-2));
        		
        		if(Board[move.row-1][move.col+1]==0)
        				
            		beforeMove.add(new Move(move.row-1,move.col+1));
            			
            	if(Board[move.row-1][move.col+2]==0)
            				
                	beforeMove.add(new Move(move.row-1,move.col+2));
            	
            	
            	if(Board[move.row+1][move.col]==0)
    				
        			beforeMove.add(new Move(move.row+1,move.col));
        		
        		if(Board[move.row+1][move.col-1]==0)
    				
        			beforeMove.add(new Move(move.row+1,move.col-1));
        			
        		if(Board[move.row+1][move.col-2]==0)
        				
            		beforeMove.add(new Move(move.row+1,move.col-2));
        		
        		if(Board[move.row+1][move.col+1]==0)
        				
            		beforeMove.add(new Move(move.row+1,move.col+1));
            			
            	if(Board[move.row+1][move.col+2]==0)
            				
                	beforeMove.add(new Move(move.row+1,move.col+2));
            	
            	if(Board[move.row-2][move.col]==0)
    				
        			beforeMove.add(new Move(move.row-2,move.col));
        		
        		if(Board[move.row-2][move.col-1]==0)
    				
        			beforeMove.add(new Move(move.row-2,move.col-1));
        			
        		if(Board[move.row-2][move.col-2]==0)
        				
            		beforeMove.add(new Move(move.row-2,move.col-2));
        		
        		if(Board[move.row-2][move.col+1]==0)
        				
            		beforeMove.add(new Move(move.row-2,move.col+1));
            			
            	if(Board[move.row-2][move.col+2]==0)
            				
                	beforeMove.add(new Move(move.row-2,move.col+2));
            	
            	
            	if(Board[move.row+2][move.col]==0)
    				
        			beforeMove.add(new Move(move.row+2,move.col));
        		
        		if(Board[move.row+2][move.col-1]==0)
    				
        			beforeMove.add(new Move(move.row+2,move.col-1));
        			
        		if(Board[move.row+2][move.col-2]==0)
        				
            		beforeMove.add(new Move(move.row+2,move.col-2));
        		
        		if(Board[move.row+2][move.col+1]==0)
        				
            		beforeMove.add(new Move(move.row+2,move.col+1));
            			
            	if(Board[move.row+2][move.col+2]==0)
            				
                	beforeMove.add(new Move(move.row+2,move.col+2));
            	
            	
    		
            	/*HashSet<Move> set = new HashSet<Move>(beforeMove);
		ArrayList<Move> beforeMove2 = new ArrayList<Move>(set);*/
		
		
		return beforeMove;
    }

	public int[][] GetCurrentState() {
		

    	return BoardNow;

	}

	public int[][]GetNextState(int[][] Board,Move move,int Color){

		int[][] reBoard = new int[20][20];
		for (int i = 1; i < 20; i++)
	        for (int j = 1; j < 20; j++)
				reBoard[i][j]=Board[i][j];

    	reBoard[move.row][move.col]=Color;
    	return reBoard;

	}

	public void Mark(Move move) { //自己下棋的function

	      BoardNow[move.row][move.col] = myTake;
		  MovesNow = GetAvailableMoves(BoardNow,move,MovesNow);
		  
	}


	public boolean IsGameOver(int[][]board) {


		return true;
	}

	public int GetWinningMark() {
		return 1;

	}

	public void SetMarkTake(int color) {
		myTake = color;

	}
	public int GetMarkTake() {

		if (myTake == 1)
		      return 1;
		  else
			  return 2;
	}
	public void InputPosition() {  //對方下棋的function
		Scanner demo =new Scanner(System.in);
		System.out.println("請輸入列值(row)，格式為數字:");
        int row = demo.nextInt();
        System.out.println("請輸入行值(col)，格式為大寫英文字:");
        char col = demo.next().charAt(0);
	      int charToInt = col - 'A' + 1 ;

	      if ( BoardNow[row][charToInt] == 1 || BoardNow[row][charToInt] == 2 ) {
	    	  System.out.println("這個點已經被下過了,請重新輸入一個點:");
	    	  InputPosition();
	      }
	      else
	    	  if ( myTake == 1  )
	    	      BoardNow[row][charToInt] = 2;
	    	  else
	    		  BoardNow[row][charToInt] = 1;
	}
	public void PrintBoard(int[][] Board) {

		System.out.print("  ");
		for (int i = 1 ; i <= 19 ; i++) {
			 System.out.print(i);
		     System.out.print(" ");
		}
		System.out.println();
		System.out.print('A');
		int count = 1;
		for (int y = 1 ; y <= 19 ; y++ )
    	  for (int x = 1 ; x <= 19 ; x++ ) {
    		 if (x<=10)
				if(Board[x][y]==3)
					System.out.print(" " + 0);
				else
					System.out.print(" " + Board[x][y]);
    		 else
				if(Board[x][y]==3)
					System.out.print("  " + 0);
				else
					System.out.print("  " + Board[x][y]);
		     if ( x == 19 && y != 19) {
		    	 System.out.println();
		         System.out.print( (char)('A' + count));
		         ++count;
		     }
    	 }
		System.out.println();
	}



}
