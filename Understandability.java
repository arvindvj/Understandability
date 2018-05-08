import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.sql.*;

public class Understandability {
	long cycloComp;
	double nestedBlock;
	long countParams;
	long countStmnts;
	long countAssigns;
	long countBLines;
	long countChars;
	long countCommas;
	long countComnts;
	long countCompare;
	long countCondl;
	long countIdentifiers;
	long countKeywords;
	int countLiterals;
	long countLoops;
	long countNums;
	long countOps;
	long countParen;
	long countPeriods;
	long countSpaces;
	long countStrings;
	long countWords;
	int mxIndent;
	double avgIndent;
	long idenLength;
	long lineLength;
	int extAlignBl;
	int countAlignBl;
	HashMap<String, Integer> opr = new HashMap<String, Integer>();
    HashMap<String, Integer> opd = new HashMap<String, Integer>();
	int LOC;
	double Entropy;
	double Volume;
	double NMI;
	double NM;
	double ITID;
	double TC;
	double Readability;
	
	public Understandability(String fileName, Connection c, PreparedStatement p) {
		cycloComp = CyclomaticComp(fileName);
		nestedBlock = Indent_NestedB(fileName);
		countParams = CountParameters(fileName);
		countCondl = CountConditionalStatements(fileName);
		countStmnts = CountStmnts(fileName) + countCondl;
		countAssigns = CountAssignmentOperators(fileName);
		countBLines = CountBlankLines(fileName);
		countChars = CountCharacters(fileName);
		countCommas = CountCommas(fileName);
		countComnts = CountComments(fileName);
		countCompare = CountRelationalOperators(fileName);
		countIdentifiers = CountIdentifiers(fileName);
		countKeywords = CountKeywords(fileName);
		countLoops = CountLoops(fileName);
		countNums = CountNumbers(fileName);
		countOps = CountRelationalOperators(fileName);
		countOps += countAssigns;
		countOps += CountArithmeticOperators(fileName);
		countOps += CountBitwiseOperators(fileName);
		countOps += CountLogicalOperators(fileName);
		countParen = CountParenthesis(fileName);
		countPeriods = CountPeriods(fileName);
		countSpaces = CountSpaces(fileName);	
		countStrings = CountStrings(fileName);
		countWords = CountWords(fileName);
		lineLength = countChars;
		Read_Metrics(fileName);
		Readability(fileName);
		GSRM(fileName);
		CS(fileName);
		
		try {
			p.setLong(1,cycloComp);
			p.setDouble(2,nestedBlock);
			p.setLong(3,countParams);
			p.setLong(4,countStmnts);
			p.setLong(5,countAssigns);
			p.setLong(6,countBLines);
			p.setLong(7,countChars);
			p.setLong(8,countCommas);
			p.setLong(9,countComnts);
			p.setLong(10,countCompare);
			p.setLong(11,countCondl);
			p.setLong(12,countIdentifiers);
			p.setLong(13,countKeywords);
			p.setInt(14,countLiterals);
			p.setLong(15,countLoops);
			p.setLong(16,countNums);
			p.setLong(17,countOps);
			p.setLong(18,countParen);
			p.setLong(19,countPeriods);
			p.setLong(20,countSpaces);
			p.setLong(21,countStrings);
			p.setLong(22,countWords);
			p.setInt(23,mxIndent);
			p.setLong(24,idenLength);
			p.setLong(25,lineLength);
			p.setInt(26,countAlignBl);
			p.setInt(27,extAlignBl);
			p.setDouble(28,Entropy);
			p.setInt(29,LOC);
			p.setDouble(30,Volume);
			p.setDouble(31,NMI);
			p.setDouble(32,NM);
			p.setDouble(33,ITID);
			p.setDouble(34,TC);
			p.setDouble(35,Readability);
			p.setString(36,fileName);
			
			p.addBatch();
		}
		catch(Exception e) {
			System.out.println("Exception while inserting record for: "+fileName);
			System.out.println("Exception "+e);
		}
	}

	public static void main(String[] args) {		
		try {
			postgresConnection con=new postgresConnection();
			Connection c= con.startConnection();
			c.setAutoCommit(false);
			Statement stmt = c.createStatement();
	        Set<String> filePaths=new HashSet<String>();
	        filePaths.add("example1.java");
	        filePaths.add("example2.java");
	        String sql="INSERT INTO understandability (cycloComp, nestedBlock, countParams, countStmnts, countAssigns, countBLines,"
	        		+ "countChars, countCommas, countComnts, countCompare, countCondl, countIdentifiers, countKeywords, countLiterals,"
	        		+ "countLoops, countNums, countOps, countParen, countPeriods, countSpaces, countStrings, countWords, mxIndent, idenLength,"
	        		+ "lineLength, countAlignBl, extAlignBl, Entropy, LOC, Volume, NMI, NM, ITID, TC, Readability, file_path) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
	        PreparedStatement ps = c.prepareStatement(sql);
	        for(String s:filePaths)
	        {
	        	Understandability obj = new Understandability(s, c, ps);
	        	obj.getClass();
	        }
	        ps.executeBatch(); 
	        c.commit();
	        stmt.close();
	        c.close();
		}
		catch (Exception e){
			System.out.println(e);
		}		
	}
	
	private long CyclomaticComp(String fileName) {
		//Calculate the Cyclomatic Complexity of the parsed java file
		//Absolute path of the accompanied lizard file
        String lizardPath = "lizard.py";
        String command = "python " + lizardPath + " " + fileName;        
        long output = 0;
        Process p;
        try {
            p = Runtime.getRuntime().exec(command);
            p.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));

            String line = "";
            int cnt = 1;
            while ((line = reader.readLine()) != null) {
            	if(cnt == 4) {
            		output = Long.parseLong(line.trim().split(" +")[1]);
            		break;
            	}
            	cnt++;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return output;
	}

	private double Indent_NestedB(String fileName) {
		/* Calculate Indentation Length and Nested Blocks depth of the parsed java file */
		int indentCount = 0, maxIndent = 0, current = 0, prev = 0, lineCount = 0, nested = 0, nestedTotal = 0;
		int space = 1, first = 0;
		Scanner in = null;
		try {
			in = new Scanner(new File(fileName));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		String line;
		while (in.hasNextLine()) {
			line = in.nextLine();
			if (line.length() > 0) {
				int i = 0;
				current = 0;
				
				while (i < line.length() && (line.charAt(i) == '\t' || line.charAt(i) == ' ')) {
					if (line.charAt(i) == '\t') 
						first = 1;
					current += 1;
					i += 1;
				}
				if (first == 0 && i > 1) {
					space = i;
					first = 1;
				}
				current = current / space;
				
				if (nested > 0 && current < prev) {
					nested -= 1;
				}
				nestedTotal += nested;
				if ((i+5 < line.length() && line.substring(i, i+5).equals("while"))
						|| (i+3 < line.length() && line.substring(i, i+3).equals("for"))
						|| (i+3 < line.length() && line.substring(i, i+3).equals("try"))
						|| (i+2 < line.length() && line.substring(i, i+2).equals("if"))
						|| (i+4 < line.length() && line.substring(i, i+4).equals("else"))) {
					nested += 1;
				}
				
				if (current > maxIndent) {
					maxIndent = current;
				}
				indentCount += current;
				prev = current;
				lineCount += 1;
			}
		}
		in.close();
		avgIndent = (double)indentCount / (double)lineCount;
		mxIndent = maxIndent;
		return (double)nestedTotal / (double)lineCount;
	}

	private long CountParameters(String fileName) {
		/* Counts number of parameters in the parsed java file */
		String line = "";
	    long count = 0;
	    try {
	        BufferedReader br = new BufferedReader(new FileReader(fileName));
	        Pattern p = Pattern.compile("\\((.*?)\\)");
	        
	        while ((line = br.readLine()) != null) {
	        	Matcher m = p.matcher(line);
		        while (m.find()) {
		        	if(m.group(1).contains(" "))
		        		count += m.group(1).split(",").length;
		        }
	        }
	        br.close();
	    } catch (Exception e) {
	    	System.out.println("Exception: " + e);
	    }
		return count;
	}
	
	private long CountStmnts(String fileName) {
		/* Counts number of statements in the parsed java file */
		char character = ';';
		long count = 0;
		try {
	        BufferedReader reader = new BufferedReader(new FileReader(fileName));
	        String line = null;
	        while ((line = reader.readLine()) !=null) {
	            for(int i=0; i<line.length();i++){
	                if(line.charAt(i) == character){
	                    count++;
	                }
	            }
	        }
	        reader.close();
	    } catch (Exception e) {
	    	System.out.println("Exception: " + e);
	    }
		return count;
	}
	
	private long CountBlankLines(String fName) {
		/* Counts number of blank lines in the parsed java file */
		BufferedReader reader = null;
		long numBlankLines = 0;
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				if(currentLine.trim().isEmpty())
				{
					numBlankLines += 1;
				}	
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in CountBlankLines: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return numBlankLines;
	}
	
	private long CountCharacters(String fileName) {
		/* Counts number of characters in the parsed java file */
		long charCount = 0;
		Scanner in = null;
		try {
			in = new Scanner(new File(fileName));
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
		String line;
		while (in.hasNextLine()) {
			line = in.nextLine();
			charCount += line.length();
		}
		in.close();
		return charCount;
	}
	
	private long CountCommas(String fileName) {
		/* Counts number of commas in the parsed java file */
		char character = ',';
		long count = 0;
		try {
	        BufferedReader reader = new BufferedReader(new FileReader(fileName));
	        String line = null;
	        while ((line = reader.readLine()) !=null) {
	            for(int i=0; i<line.length();i++){
	                if(line.charAt(i) == character){
	                    count++;
	                }
	            }
	        }
	        reader.close();
	    } catch (Exception e) {
	    	System.out.println("Exception: " + e);
	    }
		return count;
	}
	
	private long CountComments(String fileName) {
		/* Counts number of comments in the parsed java file */
		String line = "";
	    long mlcount = 0, slcount = 0;
	    try {
	        BufferedReader br = new BufferedReader(new FileReader(fileName));
	        while ((line = br.readLine()) != null) {
	            if (line.contains("//")) {
	                slcount++;
	            } else if (line.contains("/*")) {
	                mlcount++;
	            }
	        }
	        br.close();
	    } catch (Exception e) {
	    	System.out.println("Exception: " + e);
	    }
		return mlcount+slcount;
	}
	
	private long CountConditionalStatements(String fName) {
		/* Counts number of Conditional statements in the parsed java file which is the summation of number of IF statements and CASE statements*/
		BufferedReader reader = null;
		long numIfStmts = 0;
		long numCaseStmts=0;
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				if(currentLine.trim().isEmpty())
				{
					continue;
				}
				String arr[]=currentLine.split(" ");
				for(int i=0;i<arr.length;i++)
				{
					if(arr[i].equals("if"))
					{
						numIfStmts += 1;	
					}
					else if (arr[i].equals("case"))
					{
						numCaseStmts += 1;	
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in CountConditionalStatements: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return numIfStmts+numCaseStmts;
	}

	private long CountIdentifiers(String fileName) {
		/* Counts number of identifiers in the parsed java file */
		String[] keywords = {"boolean","byte",
				"char","class","double",
				"enum","float",
				"int","interface","long",
				"short",
				"void"};
				
		boolean flag = false;
		long count = 0;
		try {
		       BufferedReader reader = new BufferedReader(new FileReader(fileName));
		       String line = null;
		       while ((line = reader.readLine()) !=null) {
		    	   for (String element : line.split(" ")) {
		    		   if(flag) {
		    			   String[] el = element.split("\\(")[0].split("\\)")[0].split("=");
		    			   idenLength += el[0].length();
		    			   flag = false;
		    		   }
		    		   else {
		    			   for(int i = 0; i < keywords.length; i++) {
		    				   if (element.equals(keywords[i])) {
		    					   count++;
		    					   flag = true;
		    				   }
		    			   }
		    		   }
		    	   }
		       }
		       reader.close();
		} catch (FileNotFoundException e) {
			// File not found
		} catch (IOException e) {
			// Couldn't read the file
		}
		return count;
	}

	private long CountKeywords(String fileName) {
		/* Counts number of keywords in the parsed java file */
		String[] keywords = {"abstract","assert","boolean","break","byte","case","catch","char","class","const","continue","default","do","double","else",
				"enum","extends","final","finally","float","for","goto","if","implements","import","instanceof","int","interface","long","native",
				"new","package","private","protected","public","return","short","static","strictfp","super","switch","synchronized","this",
				"throw","throws","transient","try","void","volatile","while"};
				
		long count = 0;
		try {
		       BufferedReader reader = new BufferedReader(new FileReader(fileName));
		       String line = null;
		       while ((line = reader.readLine()) !=null) {
		    	   for (String element : line.split(" ")) {
		    		   for(int i = 0; i < keywords.length; i++) {
			        		if (element.equals(keywords[i])) {
			        			count++;
			        		}
		    		   }
			       }
		       }
		       reader.close();
		} catch (Exception e) {
			System.out.println("Exception: " + e);
		}
		return count;
	}
	
	private long CountLoops(String fName) {
		/* Counts number of loops in the parsed java file */
		BufferedReader reader = null;
		Set<String> loops = new HashSet<String>(Arrays.asList("for","while"));
		long numLoops = 0;
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				if(currentLine.trim().isEmpty())
				{
					continue;
				}
				String arr[]=currentLine.split(" ");
				for(int i=0;i<arr.length;i++)
				{
					if(loops.contains(arr[i]))
					{
						numLoops += 1;	
					}
				}	
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in CountLoops: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return numLoops;	
	}
	
	private long CountNumbers(String fileName) {
		/* Counts number of numerals in the parsed java file */
		long count = 0;
		try {
	        BufferedReader reader = new BufferedReader(new FileReader(fileName));
	        String line = null;
	        while ((line = reader.readLine()) !=null) {
	            for(int i=0; i<line.length();i++){
	            	if (Character.isDigit(line.charAt(i))) {
	                    count++;
	                }
	            }
	        }
	        reader.close();
	    } catch (Exception e) {
	    	System.out.println("Exception: " + e);
	    }
		return count;
	}
	
	private long CountRelationalOperators(String fName) {
		/* Counts number of relational operators in the parsed java file */
		BufferedReader reader = null;
		Set<String> relationalOperators = new HashSet<String>(Arrays.asList("==", ">=", "!=","<=","<",">"));
		long numRelationalOp = 0;
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				if(currentLine.trim().isEmpty())
				{
					continue;
				}
				String arr[]=currentLine.split(" ");
				for(int i=0;i<arr.length;i++)
				{
					if(relationalOperators.contains(arr[i]))
					{
						numRelationalOp += 1;	
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in CountRelationalOperators: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return numRelationalOp;
	}
	
	private long CountAssignmentOperators(String fName) {
		/* Counts number of assignment operators in the parsed java file */
		BufferedReader reader = null;
		Set<String> assignmentOperators = new HashSet<String>(Arrays.asList("=","+=","-=","*=","/=","%=","<<=",">>=","&=","|=","^="));
		long numAssignmentOp = 0;
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				if(currentLine.trim().isEmpty())
				{
					continue;
				}
				String arr[]=currentLine.split(" ");
				for(int i=0;i<arr.length;i++)
				{
					if(assignmentOperators.contains(arr[i]))
					{
						numAssignmentOp += 1;	
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in CountAssignmentOperators: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return numAssignmentOp;
	}
	
	private long CountArithmeticOperators(String fName) {
		/* Counts number of arithmetic operators in the parsed java file */
		BufferedReader reader = null;
		Set<String> arithmeticOperators = new HashSet<String>(Arrays.asList("+","-","*","/","%","++","--"));
		long numArithmeticOp = 0;
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				if(currentLine.trim().isEmpty())
				{
					continue;
				}
				String arr[]=currentLine.split(" ");
				for(int i=0;i<arr.length;i++)
				{
					if(arithmeticOperators.contains(arr[i]))
					{
						numArithmeticOp += 1;	
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in CountArithmeticOperators: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return numArithmeticOp;
	}
	
	private long CountBitwiseOperators(String fName) {
		/* Counts number of bitwise operators in the parsed java file */
		BufferedReader reader = null;
		Set<String> bitwiseOperators = new HashSet<String>(Arrays.asList("&","|","^","~","<<",">>",">>>"));
		long numBitwiseOp = 0;
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				if(currentLine.trim().isEmpty())
				{
					continue;
				}
				String arr[]=currentLine.split(" ");
				for(int i=0;i<arr.length;i++)
				{
					if(bitwiseOperators.contains(arr[i]))
					{
						numBitwiseOp += 1;	
					}
				}
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in CountBitwiseOperators: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return numBitwiseOp;
	}
	
	private long CountLogicalOperators(String fName) {
		/* Counts number of logical operators in the parsed java file */
		BufferedReader reader = null;
		Set<String> logicalOperators = new HashSet<String>(Arrays.asList("&&","||","!"));
		long numLogicalOp = 0;
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				if(currentLine.trim().isEmpty())
				{
					continue;
				}
				String arr[]=currentLine.split(" ");
				for(int i=0;i<arr.length;i++)
				{
					if(logicalOperators.contains(arr[i]))
					{
						numLogicalOp += 1;	
					}
				}
			}	
		}
		catch(Exception e)
		{
			System.out.println("Exception in CountLogicalOperators: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return numLogicalOp;
	}
	
	private long CountParenthesis(String fileName) {
		/* Counts number of parenthesis in the parsed java file */
		char l_paren = '(';
		char l_brace = '{';
		char l_bracket = '[';
		char r_paren = ')';
		char r_brace = '}';
		char r_bracket = ']';
		
		long lp_count = 0;
		long rp_count = 0;
		long lb_count = 0;
		long rb_count = 0;
		long lbk_count = 0;
		long rbk_count = 0;
		
		try {
	        BufferedReader reader = new BufferedReader(new FileReader(fileName));
	        String line = null;
	        while ((line = reader.readLine()) !=null) {
	            for(int i=0; i<line.length();i++){
	                if(line.charAt(i) == l_paren){
	                	lp_count++;
	                }
	                if(line.charAt(i) == l_brace){
	                	lb_count++;
	                }
	                if(line.charAt(i) == l_bracket){
	                	lbk_count++;
	                }
	                if(line.charAt(i) == r_paren){
	                	rp_count++;
	                }
	                if(line.charAt(i) == r_brace){
	                	rb_count++;
	                }
	                if(line.charAt(i) == r_bracket){
	                	rbk_count++;
	                }
	            }
	        }
	        reader.close();
	    } catch (Exception e) {
	    	System.out.println("Exception: " + e);
	    }
		return lp_count + lb_count + lbk_count + rp_count + rb_count + rbk_count;
	}
	
	private long CountPeriods(String fileName) {
		/* Counts number of periods in the parsed java file */
		char character = '.';
		long count = 0;
		try {
	        BufferedReader reader = new BufferedReader(new FileReader(fileName));
	        String line = null;
	        while ((line = reader.readLine()) !=null) {
	            for(int i=0; i<line.length();i++){
	                if(line.charAt(i) == character){
	                    count++;
	                }
	            }
	        }
	        reader.close();
	    } catch (Exception e) {
	    	System.out.println("Exception: " + e);
	    }
		return count;
	}
	
	private long CountSpaces(String fName) {
		/* Counts number of spaces in the parsed java file which is the summation of number of IF statements and CASE statements*/
		BufferedReader reader = null;
		
		long spaceCount=0;
		
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				spaceCount = spaceCount + currentLine.length() - currentLine.replaceAll(" ", "").length();	
			}
		}
		catch(Exception e)
		{
		System.out.println("Exception in CountSpaces: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}
		return spaceCount;	
	}

	private long CountStrings(String fileName) {
		/* Counts number of strings in the parsed java file */
		String line = "";
	    long count = 0;
	    try {
	        BufferedReader br = new BufferedReader(new FileReader(fileName));
	        Pattern p = Pattern.compile("\"([^\"]*)\"");
	        
	        while ((line = br.readLine()) != null) {
	        	Matcher m = p.matcher(line);
		        while (m.find()) {
		          ++count;
		        }
	        }
	        br.close();
	    } catch (Exception e) {
	    	System.out.println("Exception: " + e);
	    }
	    return count;
	}

	private long CountWords(String fName) {
		/* Counts number of words in the parsed java file which is the summation of number of IF statements and CASE statements*/
		BufferedReader reader = null;
		long wordCount=0;
		try
		{	if("".equals(fName))
			{
				throw new Exception("File Name is Blank, please pass the name of the file to be parsed");
			}
			reader = new BufferedReader(new FileReader(fName));
			String currentLine ;
			while ((currentLine=reader.readLine()) != null)
			{
				if(currentLine.trim().isEmpty())
				{
					continue;
				}
				String arr[]=currentLine.split(" ");
				wordCount = wordCount + arr.length;
			}
		}
		catch(Exception e)
		{
			System.out.println("Exception in CountWords: " + e);
		}
		finally
		{
			try 
			{
				reader.close();           //Closing the reader
			}
			catch (IOException e) 
			{
				e.printStackTrace();
			}
		}		
		return wordCount;	
	}
	
	private void CS(String fileName) {
		/* Calculates LOC, Entropy and Volume of the parsed java file */
		String path = fileName;
	    int lines = 0;
	    HashMap<String, Integer> hmp = new HashMap<String, Integer>();
	    String REG  = "\\w*[A-Za-z_]\\w*";                                  // REG, token, parameters
	    String REG1 = "^\\s*//";                                            // REG, lines begin with "//"
	    String REG2 = "^\\s*/\\*";                                          // REG, lines begin with "/*"
	    String REG3 = "\\*/\\s*$";                                          // REG, lines end with "*/"
	    String REG4 = "/\\*";                                               // REG, comments begin with "/*"
	    String REG5 = "\\*/";                                               // REG, comments end with "*/"
	    String REG6 = "[A-Za-z_]\\w*\\s*\\(";                               // REG, operators
	    String REG7 = "\\((\\s*\\w*[A-Za-z_]\\w*\\s*\\,)*(\\s*\\w*[A-Za-z_]\\w*\\s*){0,1}\\)";
	    
	    File inputFile = new File(path);
        Pattern p  = Pattern.compile(REG);
        Pattern p1 = Pattern.compile(REG1);
        Pattern p2 = Pattern.compile(REG2);
        Pattern p3 = Pattern.compile(REG3);
        Pattern p4 = Pattern.compile(REG4);
        Pattern p5 = Pattern.compile(REG5);
        boolean comment = false;
        try{
            Scanner newLine = new Scanner(inputFile);
            while(newLine.hasNextLine()){
                String line = newLine.nextLine();
                Matcher m = p.matcher(line);
                while(m.find()){
                    String key = m.toMatchResult().group();
                    if(hmp.containsKey(key)){
                        hmp.put(key, hmp.get(key)+1);
                    }
                    else
                        hmp.put(key, 1);
                }
                
                Matcher m1 = p1.matcher(line);
                Matcher m2 = p2.matcher(line);
                Matcher m3 = p3.matcher(line);
                Matcher m4 = p4.matcher(line);
                Matcher m5 = p5.matcher(line);
                if(comment){
                    if(m3.find())
                        comment = false;
                    else if(m5.find()){
                        comment = false;
                        ++lines;
                        String s = line.substring(m5.end());
                        matchOperators(s,REG,REG6,REG7);
                    }
                }
                else{
                    if(!m1.find()){
                        if(m2.find())
                            comment = true;
                        else if(m4.find()){
                            comment = true;
                            ++lines;
                            String s = line.substring(0, m4.start()-1);
                            matchOperators(s,REG,REG6,REG7);
                        }
                        else{
                            ++lines;
                            matchOperators(line,REG,REG6,REG7);
                        }
                    }
                }
            }
            newLine.close();
        }
        catch(Exception e){
            System.out.println(e.toString());
        }
        
        LOC = lines;
        
        double total = 0;
        for(int val : hmp.values()){
            total += val;
        }
        double entropy = 0;
        for(int val : hmp.values()){
            entropy -= (val/total) * Math.log(val/total) / Math.log(2);
        }
        Entropy = entropy;
        
        int ProgramVocabulary = opr.size() + opd.size();
        int ProgramLength = 0;
        for(int val : opr.values())
            ProgramLength += val;
        for(int val : opd.values())
            ProgramLength += val;
        
        Volume = ProgramLength * Math.log(ProgramVocabulary) / Math.log(2);
        
	}
	
	private void matchOperators(String line, String REG, String REG6, String REG7){
		/* Helper function to calculate LOC, Entropy and Volume of the parsed java file */
        Pattern p6 = Pattern.compile(REG6);
        Matcher m6 = p6.matcher(line);
        Pattern p7 = Pattern.compile(REG7);
        Matcher m7 = p7.matcher(line);
        while(m6.find()){
            String operator = m6.toMatchResult().group();
            int i = 0;
            while(operator.charAt(i)!=' ' && operator.charAt(i)!='(')
                ++i;
            operator = operator.substring(0, i);
            if(!operator.equals("if") && !operator.equals("while") && !operator.equals("for")){
                if(opr.containsKey(operator))
                    opr.put(operator, opr.get(operator)+1);
                else
                    opr.put(operator, 1);
            }
            
        }
        while(m7.find()){
            String operands = m7.toMatchResult().group();
            Pattern p71 = Pattern.compile(REG);
            Matcher m71 = p71.matcher(operands);
            while(m71.find()){
                String operand = m71.toMatchResult().group();
                if(opd.containsKey(operand))
                    opd.put(operand, opd.get(operand)+1);
                else
                    opd.put(operand, 1);
            }
        }
    }
	
	private void GSRM(String fileName) {
		/* Calculates aligned blocks and literals of the parsed java file */
		String path = fileName;
	    String REG  = "[=><]";
	    String REG1 = "(\\b|\\b0x)\\d+(\\.\\d+)?([e|E]-?\\d+(\\.\\d+)?)?(\\b|f|F|d|D)";
	    int literalLength = 0;
	    int codeLength = 0;
	    int alignedBlockExt = 0;
	    int codeLines = 0;
	    HashMap<Integer, Integer>line_size = new HashMap<Integer, Integer>();
	    HashMap<String, Integer>literal = new HashMap<String, Integer>();
	    
	    Pattern p   = Pattern.compile(REG);
        Pattern p1  = Pattern.compile(REG1);
        File infile = new File(path);
        int hold = 1;
        int pos = -1;
        int begin = 0;
        
        try{
            Scanner newLine = new Scanner(infile);
            while(newLine.hasNextLine()){
                ++codeLines;
                String line = newLine.nextLine();
                codeLength += line.length();
                Matcher m1 = p1.matcher(line);
                while(m1.find()){
                    String tmpLiter = m1.toMatchResult().group();
                    if(literal.containsKey(tmpLiter))
                        literal.put(tmpLiter, literal.get(tmpLiter)+1);
                    else
                        literal.put(tmpLiter, 1);
                    literalLength += m1.end() - m1.start();
                }
                Matcher m = p.matcher(line);
                if(m.find()){
                    if(m.start() == pos){
                        ++hold;
                        if(hold == 3)
                            begin = codeLines - 2;
                    }
                    else{
                        if(hold >= 3){
                            line_size.put(begin, hold);
                            alignedBlockExt += hold;
                        }
                        hold = 1;
                        pos = m.start();
                    }
                }
                else{
                    if(hold >= 3){
                            line_size.put(begin, hold);
                            alignedBlockExt += hold;
                        }
                    hold = 1;
                    pos = -1;
                }
            }
            newLine.close();
        }
        catch(Exception e){
            System.out.println(e.toString());
        }
        if(literalLength == codeLength)
        	;
        extAlignBl = alignedBlockExt;
        countLiterals = literal.size();
        countAlignBl = line_size.size();
	}
	
	private void Read_Metrics(String fileName) {
		/* Calculates NMI, NM, ITID and TC of the parsed java file */
		String jarPath = "metric_calculator.jar";
		String command = "java -jar " + jarPath + " " + fileName;        
		Process p;
		try {
			p = Runtime.getRuntime().exec(command);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = "";
			int cnt = 1;
			while ((line = reader.readLine()) != null) {
				String[] spl = line.split(" ");
				if(cnt == 4)
					NMI = Double.valueOf(spl[spl.length-1]);
				if(cnt == 2)
					NM = Double.valueOf(spl[spl.length-1]);
				if(cnt == 18)
					ITID = Double.valueOf(spl[spl.length-1]);
				if(cnt == 20) {
					TC = Double.valueOf(spl[spl.length-1]);
					break;
				}
				cnt++;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void Readability(String fileName) {
		/* Calculates Readability of the parsed java file */
		String jarPath = "readability.jar";
		String command = "java -jar " + jarPath;
		try {
		    Process process = Runtime.getRuntime().exec(command);
		 
		    BufferedWriter writer = new BufferedWriter(
		            new OutputStreamWriter(process.getOutputStream()));
		    BufferedReader reader = new BufferedReader(new FileReader(fileName));
		    String line = "";
		    while ((line = reader.readLine()) != null) {
				writer.write(line);
			}
		    writer.write("###");
		    writer.close();
		 
		    BufferedReader r = new BufferedReader(new InputStreamReader(
		            process.getInputStream()));
		    String ln = "";
		    int cnt = 1;
		    while ((ln = r.readLine()) != null) {
		    	if(cnt == 3) {
		    		Readability = Double.valueOf(ln);
		    		break;
		    	}
		    	cnt++;
		    }
		    reader.close();
		} catch (IOException e) {
		    e.printStackTrace();
		}
	}
}
