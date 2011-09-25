package com.bakingcode.automappingfromsql;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Main {

	// CONFIG
	/**
	 * The SQL script to parse (PARAM 1)
	 */
	private static String sqlScript;
	/**
	 * This var is where you want to save your mapped DOM classes (PARAM 2)
	 */
	private static String domPath;
	

	// REGEX
	private final static String PATTERN_CREATE_TABLE = "CREATE\\s*TABLE\\s*(\\w+)[^;]*;";
	private final static String PATTERN_SQL = "(\\w+)\\s*((INTEGER|TEXT|VARCHAR|LONGTEXT|BLOB|DATE|TIME|BOOLEAN|FLOAT|DOUBLE|BIGINT|TINYINT))[^,]*,";
	private final static String PATTERN_ALTER_TABLE = "ALTER\\s*TABLE\\s*(\\w+)\\s*ADD\\s*FOREIGN\\s*KEY\\s*\\w+\\s*\\((\\w+)\\)\\s*REFERENCES\\s*(\\w+)";

	// PRIVATE CONSTANTS
	private final static String FK_PREFIX = "id_";
	private final static String FK_SUFFIX = "_id";
	private final static String REF_KEY = "##REF##";

	// FORMAT STRINGS
	private final static String RKOBJECTMAPPING = "RKObjectMappingProvider *prov = [[RKObjectMappingProvider new] autorelease];";
	private final static String MAPPINGCLASS = "\tRKObjectMapping* mapping = [RKObjectMapping mappingForClass:[%s class]];\n";
	private final static String INTERFACE_START = "#import <RestKit/RestKit.h>\n\n@interface %s : NSObject {\n\n";
	private final static String IMPLEMENTATION_START = "\n#import \"%s.h\"\n\n@implementation %s\n\n";
	private final static String MAPKEYPATHTOREL = "\t[mapping mapKeyPath:@\"%s\" toRelationship:@\"%s\" withObjectMapping:[%s getObjectMapping]];\n";
	private final static String MAPKEYPATHTOATTR = "\t[mapping mapKeyPath:@\"%s\" toAttribute:@\"%s\"];\n";
	private final static String PROPERTY_ATTR = "@property (nonatomic, %s) %s %s;\n";
	private final static String SYNTHESIZE = "@synthesize %s;\n";

	/**
	 * Gets Cocoa Type from SQL type.
	 * 
	 * @param typeField
	 *            SQL type (INTEGER, TINYINT.... etc)
	 * @param isNotNull
	 *            if the sql type can be null or NOT
	 * @param isKey
	 *            if the field can be the key
	 * @param hasDefaultValue
	 *            if the field has a default value
	 * @return Mapped Cocoa Type
	 */
	private static String getCocoaTypeFromDB(String typeField,
			boolean isNotNull, boolean isKey, boolean hasDefaultValue) {

		String transformedType = null;

		if (typeField.equals("INTEGER") || typeField.equals("TINYINT")) {
			// Si puede ser nulo usaremos NSNumber.
			// SI es una FK dejamos el -INT-
			if (isNotNull || isKey || hasDefaultValue) {
				transformedType = "int";
			} else {
				transformedType = "NSNumber *";
			}

		} else if (typeField.equals("BIGINT")) {
			// Si puede ser nulo usaremos NSNumber.
			// SI es una FK dejamos el -INT-
			if (isNotNull || isKey || hasDefaultValue) {
				transformedType = "long";
			} else {
				transformedType = "NSNumber *";
			}

		} else if (typeField.equals("TEXT") || typeField.equals("LONGTEXT")
				|| typeField.equals("VARCHAR")) {
			transformedType = "NSString *";
		} else if (typeField.equals("BLOB")) {
			transformedType = "NSData *";
		} else if (typeField.equals("DATE")) {
			transformedType = "NSDate *";
		} else if (typeField.equals("FLOAT")) {
			transformedType = "float";
		} else if (typeField.equals("DOUBLE")) {
			transformedType = "double";
		} else if (typeField.equals("BOOLEAN")) {
			transformedType = "NSNumber *";
		} else if (typeField.equals("TIME")) {
			transformedType = "NSString *";
		}

		return transformedType;
	}

	public static void main(String[] args) {

		// Get Params
		if (args == null || args.length != 2) {
			System.out.println("HELP: The program need 2 parameters to work,");
			System.out.println("1: Where is the SQL script located");
			System.out.println("2: Path to save the Objc objects");
			System.out.println("Example running:");
			System.out.println("java -jar automappingfromsql.jar /path/to/script.sql /path/to/save/DOM/");
			return;
		}
		
		// Param Assign
		sqlScript = args[0];
		domPath = args[1];
		
		// Read SQL script
		String fileStr = null;
		try {
			fileStr = FileUtils.readFileAsString(sqlScript);
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.out.println(RKOBJECTMAPPING);

		Pattern p = Pattern.compile(PATTERN_CREATE_TABLE);
		Matcher m = p.matcher(fileStr);

		while (m.find()) {

			Pattern pC = Pattern.compile(PATTERN_SQL);
			String group = m.group();
			Matcher mC = pC.matcher(group);
			String strName = checkTableName(m.group(1));

			if (strName.contains("_")) {
				String[] strSplit = strName.split("_");
				StringBuffer strBugger = new StringBuffer();

				for (String str : strSplit) {

					strBugger.append(str.substring(0, 1).toUpperCase()
							+ str.substring(1, str.length()));
				}

				strName = strBugger.toString();
			}

			StringBuffer strInterface = new StringBuffer();
			StringBuffer strImpl = new StringBuffer();
			StringBuffer properties = new StringBuffer();
			StringBuffer synthesizes = new StringBuffer();

			// Mapping vars
			StringBuffer strMappingMethodStr = new StringBuffer();

			strMappingMethodStr.append(String.format(MAPPINGCLASS, strName));

			strInterface.append(String.format(INTERFACE_START, strName));
			strImpl.append(String
					.format(IMPLEMENTATION_START, strName, strName));

			while (mC.find()) {

				String strTrimmed = mC.group(1).trim();
				// if the field name is id, change it for key because of objc
				// reserved word "id"
				String nameField = strTrimmed.equals("id") ? "key" : strTrimmed;

				String typeField = mC.group(2).trim();

				// Gets cocoa type of mapped sql type
				String transformedType = getCocoaTypeFromDB(typeField, mC
						.group(0).contains("NOT NULL"),
						strTrimmed.contains(FK_PREFIX),
						strTrimmed.contains(" DEFAULT "));

				if (strTrimmed.contains(FK_PREFIX)
						|| strTrimmed.contains(FK_SUFFIX)) {
					strMappingMethodStr.append(String.format(MAPKEYPATHTOREL,
							REF_KEY, strTrimmed, REF_KEY));
				} else {
					strMappingMethodStr.append(String.format(MAPKEYPATHTOATTR,
							strTrimmed, nameField));
				}

				strInterface.append("\t" + transformedType + " " + nameField
						+ ";\n");

				// pointer type
				if (transformedType.contains("*")) {
					properties.append(String.format(PROPERTY_ATTR, "retain",
							transformedType, nameField));
				} else {
					properties.append(String.format(PROPERTY_ATTR, "readwrite",
							transformedType, nameField));
				}

				synthesizes.append(String.format(SYNTHESIZE, nameField));
			}

			strMappingMethodStr.append("\n\treturn mapping;\n}");
			String path = domPath + strName;

			// h
			FileUtils
					.writeFile(
							path + ".h",
							strInterface.toString()
									+ "\n}\n\n"
									+ "+ (NSString*) getKeyPath;\n+ (RKObjectMapping*) getObjectMapping;\n\n"
									+ properties.toString() + "\n@end");

			// m
			FileUtils
					.writeFile(
							path + ".m",
							strImpl.toString()
									+ synthesizes.toString()
									+ "\n+ (NSString*) getKeyPath {\n\treturn NSStringFromClass(["
									+ strName
									+ " class]);\n}\n\n+ (RKObjectMapping*) getObjectMapping {\n\n"
									+ strMappingMethodStr.toString()
									+ "\n\n\n\n@end");

			System.out.println("[prov setMapping:[" + strName
					+ " getObjectMapping]  forKeyPath:[" + strName
					+ " getKeyPath]];");

		}

		System.out.println("[objectManager setMappingProvider:prov];");
		System.out.println("[RKObjectManager setSharedManager:objectManager];");

		// Add relations
		Pattern palter = Pattern.compile(PATTERN_ALTER_TABLE);
		Matcher malter = palter.matcher(fileStr);

		while (malter.find()) {

			String interfacePathReplace = domPath
					+ checkTableName(malter.group(1)) + ".h";

			try {
				// Interfaz, imports y properties
				String strInterfaceToRead = FileUtils
						.readFileAsString(interfacePathReplace);

				String nameAttr = malter.group(2);
				String typeDestAttr = checkTableName(malter.group(3));

				// Replace prefix
				String strRepl = nameAttr.replace(FK_PREFIX, "");
				strInterfaceToRead = strInterfaceToRead.replaceAll(
						"readwrite\\)\\s*(int|long)\\s*" + FK_PREFIX + strRepl,
						"retain\\) " + typeDestAttr + " *" + nameAttr);
				strInterfaceToRead = strInterfaceToRead.replaceAll(
						"\\s*(int|long)\\s*" + FK_PREFIX + strRepl, "\n\t"
								+ typeDestAttr + " *" + nameAttr);

				// Replace suffix
				String strSufixRepl = nameAttr.replace(FK_SUFFIX, "");
				strInterfaceToRead = strInterfaceToRead.replaceAll(
						"readwrite\\)\\s*(int|long)\\s*" + strSufixRepl
								+ FK_SUFFIX, "retain\\) " + typeDestAttr + " *"
								+ nameAttr);
				strInterfaceToRead = strInterfaceToRead.replaceAll(
						"\\s*(int|long)\\s*" + strSufixRepl + FK_SUFFIX, "\n\t"
								+ typeDestAttr + " *" + nameAttr);

				strInterfaceToRead = "#import \"" + typeDestAttr + ".h\"\n"
						+ strInterfaceToRead;

				FileUtils.writeFile(interfacePathReplace, strInterfaceToRead);

				// Replace implementation map relations
				String implementationPathReplace = domPath
						+ checkTableName(malter.group(1)) + ".m";

				try {
					String strImplementationToRead = FileUtils
							.readFileAsString(implementationPathReplace);

					// Add mapping to relations
					strImplementationToRead = strImplementationToRead
							.replaceFirst(REF_KEY, typeDestAttr);
					strImplementationToRead = strImplementationToRead
							.replaceFirst(REF_KEY, typeDestAttr);

					FileUtils.writeFile(implementationPathReplace,
							strImplementationToRead);

				} catch (IOException e) {
					e.printStackTrace();
				}

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

	}

	private static String checkTableName(String strName) {
		if (strName.contains("_")) {
			String[] strSplit = strName.split("_");
			StringBuffer strBugger = new StringBuffer();

			for (String str : strSplit) {

				strBugger.append(str.substring(0, 1).toUpperCase()
						+ str.substring(1, str.length()));
			}

			return strBugger.toString();
		}
		return strName;
	}

}
