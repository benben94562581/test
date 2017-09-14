import java.io.File;
import java.io.FileInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;

public class TestStatus implements ClassFileTransformer {
	private final static String prefix = "\nlong startTime = System.nanoTime();\n";
	private final static String postfix = "\nlong endTime = System.nanoTime();\n";
	private final static List<String> classList = new ArrayList<String>();
	private final static List<String> objmethod = new ArrayList<String>();
	private final static String fileName = "monitor.properties";
	private final static String monitorpackage = "monitorlist";
	private final static String monitorClassPath = "monitorclasspath";
	private final static ClassPool pool = ClassPool.getDefault();
//	private final static String displayZero = "displayzero";
	
	private final static String displaytime = "displaytime";
	private static int timedisplay = 0;
//	private static String displayzero = "false";
	private static String deployInJbossProperty = "deployInJboss";
	private static String jbossroot = "jbossroot";
	private static String jbossrootpath = "";
	private static boolean inJbossFirst = false;
	private final static String output = "output";
	private static int outputlevel = 1;

	static {
		readProperty();
		try {
			CtClass ctclass1 = ClassPool.getDefault().get("java.lang.Object");
			CtMethod[] methods = ctclass1.getMethods();
			for (CtMethod t : methods) {
				objmethod.add(t.getName());
			}
			objmethod.add("main");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private static void setPoolPathForJboss() throws Exception {
		File jbossrootpathFile = new File(jbossrootpath);
		boolean needbreak = false;

		if (jbossrootpathFile.exists()) {
			File[] files = jbossrootpathFile.listFiles();
			for (File tempT : files) {
				String tempPath = tempT.getName();

				if (tempPath != null && tempPath.startsWith("temp")) {
					File tempFile = new File(jbossrootpathFile + File.separator
							+ tempPath);
					File[] filesTemp = tempFile.listFiles();
					if (filesTemp != null) {
						for (File warTemp : filesTemp) {
							if (warTemp.getName() != null
									&& warTemp.getName().contains(".war")) {

								File[] webs = warTemp.listFiles();
								for (File webfile : webs) {
									if (webfile.getName() != null
											&& webfile.getName().equals(
													"WEB-INF")) {
										String webfilepath = webfile
												.getCanonicalPath();
										String classpathes = webfilepath
												+ File.separator + "classes";
										pool.appendClassPath(classpathes);

										String libpath = webfilepath
												+ File.separator + "lib";
										File libfile = new File(libpath);
										File[] jarfiles = libfile.listFiles();
										for (File jarfile : jarfiles) {
											String jarfilename = jarfile
													.getName();
											if (jarfilename != null
													&& jarfilename
															.endsWith(".jar")) {
												pool.appendClassPath(libpath
														+ File.separator
														+ jarfilename);
											}
										}

										needbreak = true;

									}
								}
							}
							if (needbreak) {
								break;
							}
						}
						if (needbreak) {
							break;
						}
					}
					if (needbreak) {
						break;
					}
				}
				if (needbreak) {
					break;
				}
			}
		}
	}

	private static void readProperty() {
		String path = TestStatus.class.getProtectionDomain().getCodeSource()
				.getLocation().getFile();
		int index = path.lastIndexOf("/");
		if (index != -1) {
			path = path.substring(0, index);
		}
		File file = new File(path + File.separator + fileName);
		FileInputStream fis = null;
		try {
			if (file.exists()) {
				fis = new FileInputStream(file);
				Properties p = new Properties();
				p.load(fis);

				if (p.containsKey(monitorpackage)) {
					String listInStr = p.getProperty(monitorpackage);
					String[] names = listInStr.split(";");
					if (names != null && names.length > 0) {
						for (String key : names) {
							if (!"".equals(key)) {
								classList.add(key);
							}
						}
					} else if (listInStr != null && (!"".equals(listInStr))) {
						classList.add(listInStr);
					}
				}

				if (p.containsKey(deployInJbossProperty)) {
					String jboss = p.getProperty(deployInJbossProperty);
					if (jboss != null
							&& "TRUE".equalsIgnoreCase(jboss.toUpperCase())) {
						inJbossFirst = true;
					}
				}

				if (!inJbossFirst) {
					if (p.containsKey(monitorClassPath)) {
						String listclasspath = p.getProperty(monitorClassPath);
						String[] classpaths = listclasspath.split(";");
						if (classpaths != null && classpaths.length > 0) {
							for (String key : classpaths) {
								if (!"".equals(key)) {
									pool.appendClassPath(key);
									File classpathfile = new File(key);
									if (classpathfile.exists()) {
										String[] files = classpathfile.list();
										for (String jarfile : files) {
											if (jarfile.endsWith(".jar")) {
												pool.appendClassPath(key
														+ File.separator
														+ jarfile);
											}
										}
									}

								}
							}
						} else if (listclasspath != null
								&& (!"".equals(listclasspath))) {
							pool.appendClassPath(listclasspath);
						}
					}
				} else {
					if (p.containsKey(jbossroot)) {
						jbossrootpath = p.getProperty(jbossroot);
						if (p.containsKey(monitorClassPath)) {
							String listclasspath = p.getProperty(monitorClassPath);
							String[] classpaths = listclasspath.split(";");
							if (classpaths != null && classpaths.length > 0) {
								for (String key : classpaths) {
									if (!"".equals(key)) {
										pool.appendClassPath(key);
										File classpathfile = new File(key);
										if (classpathfile.exists()) {
											String[] files = classpathfile.list();
											for (String jarfile : files) {
												if (jarfile.endsWith(".jar")) {
													pool.appendClassPath(key
															+ File.separator
															+ jarfile);
												}
											}
										}

									}
								}
							} else if (listclasspath != null
									&& (!"".equals(listclasspath))) {
								pool.appendClassPath(listclasspath);
							}
						}

					}
				}
//				if (p.containsKey(displayZero)) {
//					String t = p.getProperty(displayZero);
//					if ("TRUE".equals(t.toUpperCase())) {
//						displayzero = "true";
//					}
//				}
				if(p.containsKey(displaytime)){
					String t = p.getProperty(displaytime);
					try{
						timedisplay = Integer.parseInt(t);
					}catch(Exception e){
						e.printStackTrace();
						timedisplay = 0;
					}
				}
				
				if (p.containsKey(output)) {
					String t = p.getProperty(output);
					if ("system".equals(t)) {
						outputlevel = 1;
					} else if ("log4j".equals(t)) {
						outputlevel = 2;
					} else if ("slf4j".equals(t)) {
						outputlevel = 3;
					}
				}

			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (fis != null) {
					fis.close();
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public static void premain(String agentArgs, Instrumentation inst) {
		// System.out.println("premain-1." + agentArgs);
		inst.addTransformer(new TestStatus());
	}

	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		// TODO Auto-generated method stub

		className = className.replace("/", ".");
		StringBuffer packagename = new StringBuffer();
		boolean flag = false;

		CtClass ctclass = null;
		try {
			String[] names = className.split("\\.");
			if (names != null && names.length > 0) {
				int length = names.length;
				for (int i = 0; i < length; i++) {
					if (i != 0) {
						packagename.append(".");
					}
					packagename.append(names[i]);
					if (classList.contains(packagename.toString())) {
						flag = true;
						break;
					}
				}
			} else {
				packagename.append(className);
				if (classList.contains(packagename.toString())) {
					flag = true;
				}
			}

			if (flag) {

				if (inJbossFirst) {
					setPoolPathForJboss();
					inJbossFirst = false;
				}
				ctclass = pool.get(className);

				if (!ctclass.isInterface()) {

					CtConstructor[] constructors = ctclass.getConstructors();
					String fieldName = "startTime4javassist";
					CtField f = new CtField(CtClass.longType, fieldName, ctclass);
					ctclass.addField(f);
					for(CtConstructor constructor  : constructors){
						StringBuffer outputStr = new StringBuffer(
								"\n long cost = (endTime - "+ fieldName  + ") / 1000000;\n");
						if (outputlevel == 3) {
							outputStr
									.append("org.slf4j.Logger logt = org.slf4j.LoggerFactory.getLogger(")
									.append(className).append(".class);\n");
						} else if (outputlevel == 2) {
							outputStr
									.append("org.apache.commons.logging.Log logt = org.apache.commons.logging.LogFactory.getLog(")
									.append(className).append(".class);\n");
						}
						outputStr.append("if(cost >= " + timedisplay +"){");

						if (outputlevel != 1) {
							outputStr.append("logt.error(\" ");
						} else {
							outputStr.append("System.out.println(\" ");
						}
						outputStr.append(constructor.getLongName());
						outputStr.append(" cost:\" + cost + \"ms.\");\n}\n");
//						outputStr.append("else if(" + displayzero + "){");
//						if (outputlevel != 1) {
//							outputStr.append("logt.error(\" ");
//						} else {
//							outputStr.append("System.out.println(\" ");
//						}
//						outputStr.append(constructor.getLongName());
//						outputStr.append(" cost:\" + cost + \"ms.\");\n}\n");
					
						constructor.insertBefore(fieldName + " = System.nanoTime();\n");
					
						constructor.insertAfter(postfix + outputStr); 	
						
						
					}
					
					

					
					CtMethod[] methods = ctclass.getDeclaredMethods();
					for (CtMethod method : methods) {
						String name = method.getName();
						if (objmethod.contains(name)) {
							continue;
						}

						StringBuffer outputStr = new StringBuffer(
								"\n long cost = (endTime - startTime) / 1000000;\n");
						if (outputlevel == 3) {
							outputStr
									.append("org.slf4j.Logger logt = org.slf4j.LoggerFactory.getLogger(")
									.append(className).append(".class);\n");
						} else if (outputlevel == 2) {
							outputStr
									.append("org.apache.commons.logging.Log logt = org.apache.commons.logging.LogFactory.getLog(")
									.append(className).append(".class);\n");
						}
						outputStr.append("if(cost >= " + timedisplay+"){");

						// outputStr.append("org.slf4j.Logger logt = org.slf4j.LoggerFactory.getLogger(").append(className).append(");");
						if (outputlevel != 1) {
							outputStr.append("logt.error(\" ");
						} else {
							outputStr.append("System.out.println(\" ");
						}
						outputStr.append(method.getLongName());
						outputStr.append(" cost:\" + cost + \"ms.\");\n}\n");
//						outputStr.append("else if(" + displayzero + "){");
//						if (outputlevel != 1) {
//							outputStr.append("logt.error(\" ");
//						} else {
//							outputStr.append("System.out.println(\" ");
//						}
//						outputStr.append(method.getLongName());
//						outputStr.append(" cost:\" + cost + \"ms.\");\n}\n");

						String newMethodName = method.getName()
								+ ctclass.getSimpleName() + "$impl";
						method.setName(newMethodName);
						CtMethod newMethod = CtNewMethod.copy(method, name,
								ctclass, null);
						String type = method.getReturnType().getName();
						StringBuilder sb = new StringBuilder();
						sb.append("{");
						sb.append(prefix);
						if (!"void".equals(type)) {
							sb.append(type + " result = ");
						}
						sb.append(newMethodName + "($$);\n");
						sb.append(postfix);
						sb.append(outputStr);
						if (!"void".equals(type)) {
							sb.append("return result ; \n");
						}
						sb.append("}");
						newMethod.setBody(sb.toString());
//						System.out.println(sb);
						ctclass.addMethod(newMethod);
					}

				}

			}
			if (ctclass == null) {
				return null;
			}
//			 System.out.println(ctclass);
			return ctclass.toBytecode();
		} catch (Exception e) {
			// System.out.println(ctclass);
			e.printStackTrace();
		}
		return null;
	}

}
