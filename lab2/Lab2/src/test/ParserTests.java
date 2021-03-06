package test;

import static org.junit.Assert.fail;

import java.io.StringReader;

import lexer.Lexer;

import org.junit.Test;

import parser.Parser;

public class ParserTests {
	private void runtest(String src) {
		runtest(src, true);
	}

	private void runtest(String src, boolean succeed) {
		Parser parser = new Parser();
		try {
			parser.parse(new Lexer(new StringReader(src)));
			if(!succeed) {
				fail("Test was supposed to fail, but succeeded");
			}
		} catch (beaver.Parser.Exception e) {
			if(succeed) {
				e.printStackTrace();
				fail(e.getMessage());
			}
		} catch (Throwable e) {
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	@Test
	public void testModule() {
		runtest("module Test { }", true);
		runtest("module Foo {}", true);
		runtest("module Bar { import foo; import bar; import baz; void func() {return;}}", true);
		runtest("module {}", false);
	}
	@Test
	public void testImportStmts() {
		runtest("module Bar { import foo; import bar; import baz;}", true);
		runtest("module Bar { import foo; import; import baz;}", false);
		runtest("module Bar { foo; import foo; import baz;}", false);
	}
	@Test
	public void testFuncDeclarations() {
		runtest("module Bar { void () {return;} }", false);
		runtest("module Bar { void foo() {return;}}", true);
		runtest("module Bar { int bar() {}}", true);
		runtest("module Bar { int bar(int a, String b, Int[] c, string[] d) {}}", true);
		runtest("module Bar { int bar(int a, String b, int[] c, string[] d) {}}", false);
		runtest("module Bar { int bar(int a, String b, int[] c,) {}}", false);
		runtest("module Bar { int bar(int a, String b, int[] c {}}", false);
		runtest("module Bar { public int bar(int a, String b) {}}", true);
		runtest("module Bar { publc int bar(int a, String b) {}}", false);
	}
	@Test
	public void testFieldsDeclarations() {
		runtest("module Bar { public int a;}", true);
		runtest("module Bar { String b;}", true);
		runtest("module Bar { String[] b;}", true);
		runtest("module Bar { pubd int b;}", false);
		runtest("module Bar { public int b}", false);
	}
	@Test
	public void testTypeDeclarations() {
		runtest("module Bar { type FooInt = \"something\";}", true);
		runtest("module Bar { type FooInt = 123;}", false);
	}
	@Test
	public void testLocalVariableAssignments() {
		runtest("module Bar { void foo(){int a;}}", true);
		runtest("module Bar { void foo(){int a; a=2;}}", true);
		runtest("module Bar { void foo(){int a = b;}}", false);
		runtest("module Bar { void foo(){String c =;}}", false);
		runtest("module Bar { void foo(){String c; c = \"qwerty\";}}", true);
		runtest("module Bar { void foo(){String[] d; d = [1,2,3];}}", true);
	}
	@Test
	public void testRetrnStatement() {
		runtest("module Bar { String foo(){return a;}}", true);
		runtest("module Bar { String foo(){return;}}", true);
		runtest("module Bar { String foo(){return String;}}", true);
		runtest("module Bar { String foo(){return a, b;}}", false);
	}
	@Test
	public void testBlockStatement() {
		runtest("module Bar { void foo(){ int a; {} }}", true);
		runtest("module Bar { void foo(){ int a; {{}}{} }}", true);
		runtest("module Bar { void foo(){ int a; {{}}{int b; b=3;} }}", true);
		runtest("module Bar { void foo(){ int a; {(} }}", false);
	}
	@Test
	public void testIfStatement() {
		runtest("module Bar { void foo() { if(true) {} else {}}}", true);
		runtest("module Bar { void foo() { if(true) {}}}", false);
		runtest("module Bar { void foo() { if(true) {return;} else {}}}", true);
		runtest("module Bar { void foo() { if(foo) {} else {}}}", true);
		runtest("module Bar { void foo() { if(1) {} else {}}}", true);
		
	}
	@Test
	public void testWhileStatement() {
		runtest("module Bar { void foo(){ while(true){ }}}", true);
		runtest("module Bar { void foo(){ while(true){ int a; a=3;}}}", true);
		runtest("module Bar { void foo(){ while(){ int a; a=3;}}}", false);
	}
	@Test
	public void testBreakStatement() {
		runtest("module Bar { void foo(){ break;}}", true);
		runtest("module Bar { void foo(){ break}}", false);
	}
	@Test
	public void testExprStatement() {
		runtest("module Bar { void foo(){ a == 1;}}", true);
		runtest("module Bar { void foo(){ a != 1;}}", true);
		runtest("module Bar { void foo(){ a < 1;}}", true);
		runtest("module Bar { void foo(){ a <= 1;}}", true);
		runtest("module Bar { void foo(){ a > 1;}}", true);
		runtest("module Bar { void foo(){ a >= 1;}}", true);
		runtest("module Bar { void foo(){ a=a+1;}}", true);
		runtest("module Bar { void foo(){ a=a-1;}}", true);
		runtest("module Bar { void foo(){ a=a++;}}", false);
		runtest("module Bar { void foo(){ a=a*b;}}", true);
		runtest("module Bar { void foo(){ a=a/1;}}", true);
		runtest("module Bar { void foo(){ a=a%5;}}", true);
		runtest("module Bar { void foo(){ foo();}}", true);
		runtest("module Bar { void foo(){ bar(1,2,3);}}", true);
		runtest("module Bar { void foo(){ foo(a,b,\"abc\");}}", true);
		runtest("module Bar { void foo(){ a=(a+(b/(c*(d-(1%(2))))));}}", true);
		runtest("module Bar { void foo(){ a=a+(foo+()));}}", false);
		runtest("module Bar { void foo(){ a=a+\"foo\";}}", true);
		runtest("module Bar { void foo(){ a=true;}}", true);
		runtest("module Bar { void foo(){ a=false;}}", true);
		runtest("module Bar { void foo(){ a=true+false;}}", true);
		runtest("module Bar { void foo(){ true=false;}}", false);
		runtest("module Bar { void foo(){ true=a;}}", false);
	}
}
