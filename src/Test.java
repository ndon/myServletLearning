
public class Test {
	
	public static void main(String[] args) {
		System.out.println("*************************************");
		ClassLoader loader = Test.class.getClassLoader();
		while(loader != null) {
			System.out.println(loader);
			loader = loader.getParent();
		}
		System.out.println("*************************************");
		
	}

}
