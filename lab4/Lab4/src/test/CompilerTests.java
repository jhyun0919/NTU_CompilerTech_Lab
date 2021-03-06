package test;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import backend.ProgramCodeGenerator;

import org.junit.Assert;
import lexer.Lexer;
import parser.Parser;
import soot.Printer;
import soot.SootClass;
import soot.jimple.JasminClass;
import soot.util.JasminOutputStream;
import ast.List;
import ast.Module;
import ast.Program;

/**
 * System tests for the compiler: compiles a given program to Java bytecode, then immediately
 * loads it into the running JVM and executes it.
 */
public class CompilerTests {
	// set this flag to true to dump generated Jimple code to standard output
	private static final boolean DEBUG = false;
	
	/**
	 * A simple class loader that allows us to directly load compiled classes.
	 */
	private static class CompiledClassLoader extends URLClassLoader {
		private final Map<String, byte[]> classes = new HashMap<String, byte[]>();
		
		public CompiledClassLoader() {
			super(new URL[0], CompilerTests.class.getClassLoader());
		}
		
		public void addClass(String name, byte[] code) {
			classes.put(name, code);
		}
		
		@Override
		protected Class<?> findClass(String name) throws ClassNotFoundException {
			if(classes.containsKey(name)) {
				byte[] code = classes.get(name);
				return defineClass(name, code, 0, code.length);
			}
			return super.findClass(name);
		}
	}

	/**
	 * Test runner class.
	 * 
	 * @param modules_src Array of strings, representing the source code of the program modules
	 * @param main_module the name of the main module
	 * @param main_function the name of the main function
	 * @param parm_types the parameter types of the main function
	 * @param args arguments to pass to the main method
	 * @param expected expected result
	 */
	private void runtest(String[] modules_src, String main_module, String main_function, Class<?>[] parm_types, Object[] args, Object expected) {
		try {
			List<Module> modules = new List<Module>();
			for(String module_src : modules_src) {
				Parser parser = new Parser();
				Module module = (Module)parser.parse(new Lexer(new StringReader(module_src)));
				modules.add(module);
			}
			Program prog = new Program(modules);
			
			prog.namecheck();
			prog.typecheck();
			prog.flowcheck();
			if(prog.hasErrors()) {
				Assert.fail(prog.getErrors().iterator().next().toString());
			}
			
			CompiledClassLoader loader = new CompiledClassLoader();
			try {
				for(SootClass klass : new ProgramCodeGenerator().generate(prog)) {
					if(DEBUG) {
						PrintWriter stdout_pw = new PrintWriter(System.out);
						Printer.v().printTo(klass, stdout_pw);
						stdout_pw.flush();
					}

					String name = klass.getName();
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					PrintWriter pw = new PrintWriter(new JasminOutputStream(baos));
					new JasminClass(klass).print(pw);
					pw.flush();
					loader.addClass(name, baos.toByteArray());
				}

				Class<?> testclass = loader.loadClass(main_module);
				Method method = testclass.getMethod(main_function, parm_types);
				Object actual = method.invoke(null, args);
				if(!method.getReturnType().equals(void.class))
					Assert.assertEquals(expected, actual);
			} finally {
				loader.close();
			}
		} catch(Exception e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		} catch(ClassFormatError e) {
			e.printStackTrace();
			Assert.fail(e.getMessage());
		}
	}

	/** Convenience wrapper for runtest with only a single module. Other arguments are the same .*/
	private void runtest(String string, String classname, String methodname, Class<?>[] parmTypes, Object[] args, Object expected) {
		runtest(new String[] { string }, classname, methodname, parmTypes, args, expected);
	}

	@Test public void testAddition() {
		runtest("module Test {" +
				"  public int f() {" +
				"    return 23+19;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				42);
	}

	@Test public void testSubtraction() {
		runtest("module Test {" +
				"  public int f() {" +
				"    return 42-19;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				23);
	}

	@Test public void testMultiplication() {
		runtest("module Test {" +
				"  public int f() {" +
				"    return 2*21;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				42);
	}

	@Test public void testDivision() {
		runtest("module Test {" +
				"  public int f() {" +
				"    return 42/2;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				21);
	}

	@Test public void testPrecedence() {
		runtest("module Test {" +
				"  public int f() {" +
				"    return 2+42/2;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				23);
	}

     @Test public void testParentheses() {
		runtest("module Test {" +
				"  public int f() {" +
				"    return (2+42)/2;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				22);
	}

	@Test public void testNegation() {
		runtest("module Test {" +
				"  public int f() {" +
				"    return -42;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				-42);
	}
	
	@Test public void testString() {
		runtest("module Test {" +
				"  public String f() {" +
				"  String foo;" +
				"  foo = \"qwerty\";" +
				"    return foo;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				"qwerty");
	}
	
	@Test public void testint() {
		runtest("module Test {" +
				"  public int f() {" +
				"  int foo ;" +
				"  foo = 123;" +
				"    return foo;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				123);
	}
	
	@Test public void testintAddition() {
		runtest("module Test {" +
				"  public int f() {" +
				"  int foo ;" +
				"  foo = 123+2;" +
				"    return foo + 2;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				127);
	}
	
	@Test public void testBoolean() {
		runtest("module Test {" +
				"  public boolean f() {" +
				"  boolean foo ;" +
				"  foo = true;" +
				"    return foo;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				true);
	}
	
	@Test public void testBinaryExper() {
		runtest("module Test {" +
				" public int a(){" + 
				" 	return 5;" +
				" }" +
				"  public boolean f() {" +
				"  int foo ;" +
				"  foo = 123;" +
				"    return foo > a();" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				true);
	}
	
	@Test public void testNeqExpr() {
		runtest("module Test {" +
				" public int a(){" + 
				" 	return 5;" +
				" }" +
				"  public boolean f() {" +
				"  int foo ;" +
				"  foo = 123;" +
				"    return foo != a();" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				true);
	}
	
	@Test public void testArrayIndx() {
		runtest("module Test {" +
				"  public int f() {" +
				"  int[] intArray;" +
				"  intArray = int[3];" +
				"  intArray[0] = 123;" +
				"    return intArray[0];" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				123);
	}
	
	@Test public void testArrayFunc() {
		runtest("module Test {" +
				" public int a(){" + 
				" 	return 0;" +
				" }" +
				"  public int f() {" +
				"  int[] intArray;" +
				"  intArray = int[3];" +
				"  intArray[0] = 123;" +
				"    return intArray[a()];" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				123);
	}
	
	@Test public void testMod() {
		runtest("module Test {" +
				"  public int f() {" +
				"  int foo;" +
				"  foo = 5 % 2;" +
				"    return foo;" +
				"  }" +
				"}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				1);
	}
	
	@Test public void testCallParam() {
		runtest("module Test {" +
				"  public int f(int x) {" +
				"  if (x <= 5){" +
				"	return x;" +
				"	}else{" +
				"		return f(x-1); " +
				" 	}" +
				"  }" +
				"  public int foo() {" +
				"  return f(10);" +
				"  }" +
				"}",
				"Test",
				"foo",
				new Class<?>[0],
				new Object[0],
				5);
	}
	
	@Test public void testWhile() {
		runtest("module Test {" +
				"  public int f() {" +
				"int x;"
				+ "x = 0;"
				+ "while(x<100){"
				+ "	x = x + 1;"
				+ " if (x > 10) {"
				+ "  "
				+ "} else {"
				+ "   x = x + 1;"
				+ "}"
				+ "}"
				+ "return x;"
				+ "}"
				+ "}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				100);
	}
	
	@Test public void testWhileBreak() {
		runtest("module Test {" +
				"  public int f() {" +
				"int x;"
				+ "x = 0;"
				+ "while(x<100){"
				+ "	x = x + 1;"
				+ " if (x > 10) {"
				+ "  break;"
				+ "}"
				+ "}"
				+ "return x;"
				+ "}"
				+ "}",
				"Test",
				"f",
				new Class<?>[0],
				new Object[0],
				11);
	}
}
