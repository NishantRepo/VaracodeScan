

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map.Entry;
import org.apache.log4j.Logger;
public class VeracodeClientFinal {
	final static Logger logger=Logger.getLogger(VeracodeClientFinal.class);
	private static String  SANDBOX_ID=null;
	private static String BUILD_ID=null;
	private static  String boundary;
	private static final String LINE_FEED = "\r\n";
	private static  HttpURLConnection urlConn;
	private static String charset="UTF-8";
	private static OutputStream outputStream;
	private static PrintWriter writer;
	public static void main(String[] args) throws IOException, InterruptedException {
		boundary = "===" + System.currentTimeMillis() + "===";
		System.setProperty("https.proxyHost", "proxy.statestr.com");
		System.setProperty("https.proxyPort", "80");		
		String app_id="188973";			//Veracode App id
		String appCode="ABC"; 			//eg .SDP
		String scanType="Local"; 		// certified or local
		String filePath="H:\\Binaries"; //Path of the files
		String version="v1.0.2";		//Artifactory version
		String JenkinsBuildId="04";		//Jenkins Build Id
	
	if(scanType.contains("Local")){
			version =appCode+"-"+ version+"-"+JenkinsBuildId+"-Sandbox-SDP";
			for(int i=1; i<=5; i++) {
				String sandbox_name=appCode+"-Sandbox-"+i;
				//System.out.println(sandbox_name);
				boolean newSandBox=createOrCheckSandBox(app_id, sandbox_name);

				//Create build
				boolean b =createBuildCall(app_id, version, SANDBOX_ID);
				//System.out.println(BUILD_ID);
				if(b == true) {
					break;
				}
				else if(i==5 && b==false) {
				System.out.println("Sandbox not available");
					System.exit(1);
				}

			}
		}		
		else 
			if(scanType.contains("Certified")) {
				version =appCode+"-"+ version+"-"+JenkinsBuildId+"-Policy-SDP";
				String res = getBuildInfo(app_id, null, null);
				//System.out.println(res);
				System.out.println("Waiting for scan...");
				boolean bol=validateResponse(res);
				if(!res.contains("status=\"Results Ready\"")){
					System.out.println("Policy Scan already running..");
					System.exit(1);
				}

				boolean b=createBuildCall(app_id, version, null);
				if(b ==false) {
					//System.out.println("Exit program ");
					System.exit(1);
				}
				
			} 
			else{
				System.out.println("Error please provide scan type");
			}


		// 3 Upload Files
		uploadfiles(app_id, filePath, SANDBOX_ID);

		//4 Start PreScan

		beginPreScan(app_id, SANDBOX_ID);

		// 5 Check PreScan

		pollPreScan(app_id, BUILD_ID, SANDBOX_ID);

		//6 Start Scan
		
		beginScan(app_id, "true", SANDBOX_ID);
		
		logger.info("Veracode App ID:"+app_id);
		logger.info("Veracode Build ID:"+BUILD_ID);
		logger.info("Jenkins Build UID:"+JenkinsBuildId);
		logger.info("eMail Address:"+"");
		logger.info("Scan Status:"+"Success");
		logger.info("Metrics Status:");
	}
	public static  boolean createOrCheckSandBox(String app_id, String sandbox_name) throws IOException{

		String response=getSandboxList(app_id);
		//System.out.println(response);
		//create now sand box
		String name = "sandbox_name=\""+sandbox_name+"\"";
		if(response.contains(name)) {
			int a= response.indexOf(name);
			String ss=response.substring(a-8, a);
			SANDBOX_ID=ss.toString().replace("\"", "").trim();
			//System.out.println(SANDBOX_ID);

		}else {
			String resoponse1 =createSandbox(app_id, sandbox_name);
			//System.out.println(resoponse1);
			//create now sand box
			String name1 = "sandbox_name=\""+sandbox_name+"\"";
			if(resoponse1.contains(name1)) {
				int a= resoponse1.indexOf(name1);
				String ss=resoponse1.substring(a-8, a);
				SANDBOX_ID=ss.toString().replace("\"", "").trim();
				//System.out.println(SANDBOX_ID);
			}
			return true;
		}
		return false;
	}
	public static String getBuildInfo(String app_id, String build_id, String sandbox_id){
		String url = "https://analysiscenter.veracode.com/api/5.0/getbuildinfo.do";
		HashMap<String, String > map= new HashMap<String, String>();
		map.put("app_id", app_id);
		map.put("build_id", build_id);
		map.put("sandbox_id", sandbox_id);
		String response = callURL(url,map,null);
		return response;

	}
	public static String createBuild(String app_id,String version,String sandbox_id) throws IOException{

		String url = "https://analysiscenter.veracode.com/api/5.0/createbuild.do";
		HashMap<String, String > map= new HashMap<String, String>();
		map.put("app_id", app_id);
		map.put("version", version);
		map.put("sandbox_id", sandbox_id);
		
		String response = callURL(url,map,null);
		return response;


	}
	public static String getBuildList(String app_id, String sandbox_id){
		String url = "https://analysiscenter.veracode.com/api/5.0/getbuildlist.do";
		HashMap<String, String > map= new HashMap<String, String>();
		map.put("app_id", app_id);
		map.put("sandbox_id", sandbox_id);
		String response = callURL(url,map,null);
		return response;

	}
	public static String uploadFile(String app_id, File file, String sandbox_id) throws MalformedURLException {
		String url = "https://analysiscenter.veracode.com/api/5.0/uploadfile.do";
		HashMap<String, String > map= new HashMap<String, String>();
		String response=null;
		map.put("app_id", app_id);
		map.put("sandbox_id", sandbox_id);
		response = callURL(url,map,file);
		return response;
	
	}
	public static String beginPreScan(String app_id, String sandbox_id)
	{
		String url = "https://analysiscenter.veracode.com/api/5.0/beginprescan.do";
		HashMap<String, String > map= new HashMap<String, String>();
		map.put("app_id", app_id);
		map.put("sandbox_id", sandbox_id);
		String response = callURL(url,map,null);
		return response;
	}
	public static String beginScan(String app_id, String scan_all_top_level_modules, String sandbox_id)
	{
		String url = "https://analysiscenter.veracode.com/api/5.0/beginscan.do";
		HashMap<String, String > map= new HashMap<String, String>();
		map.put("app_id", app_id);
		map.put("sandbox_id", sandbox_id);
		map.put("scan_all_top_level_modules", scan_all_top_level_modules);
		String response = callURL(url,map,null);
		return response;
	}

	public  static String createSandbox(String app_id,String sandbox_name)
	{
		String url = "https://analysiscenter.veracode.com/api/5.0/createsandbox.do";
		HashMap<String, String > map= new HashMap<String, String>();
		map.put("app_id", app_id);
		map.put("sandbox_name", sandbox_name);
		String response = callURL(url,map,null);
		return response;
	}

	public static String getSandboxList(String app_id)
	{
		String url = " https://analysiscenter.veracode.com/api/5.0/getsandboxlist.do";
		HashMap<String, String > map= new HashMap<String, String>();
		map.put("app_id", app_id);
		String response = callURL(url,map,null);
		//System.out.println(response);
		return response;
	}
	public static String findBuild(String app_id, String version, String sandbox_id) {
		String response = getBuildList(app_id, sandbox_id);
		String buildName="version=\""+version+"\"";
		if(response.contains(buildName)){
			int ind=response.indexOf(buildName);
			String build_id=response.substring(ind-9, ind-2);
			System.out.println("BUILDID is :"+build_id);
		}
		
		return null;
	}
	public static boolean createBuildCall(String app_id, String version, String sandbox_id ) throws IOException{
		System.out.println("Creating Build : "+version);
		String res;
		if(sandbox_id==null) {
			// Scan type is Certified
			//System.out.println("sandbox_id is NULL");
			res=createBuild(app_id, version, null);	
	//		System.out.println("sandbox_id is = "+sandbox_id);
			//System.out.println(res);
		}else {
			
			///Scan type is Local
			res=createBuild(app_id, version,sandbox_id);
			System.out.println("sandbox_id is = "+sandbox_id);
			//System.out.println(res);
		}
		//System.out.println(res);
		boolean result=validateResponse(res); 
		if(result == true) {
			String name="build_id";
			//String build_id=null;
			int len =2+(res.indexOf(name)+name.length());
			BUILD_ID=res.substring(len, len+7);
			System.out.println(version+" Build created. Build id is : "+BUILD_ID);
			return true;
		}else {
			
			return false;
		}

	}

	public static String uploadfiles(String app_id, String filePath, String sandbox_id) throws IOException{
		File[] files = new File(filePath).listFiles();
		String res =null;
		boolean flag=false;
		//If this pathname does not denote a directory, then listFiles() returns null. 
		if(sandbox_id ==null) {
			for (File file : files) {
				if (file.isFile()) {
					String fileName=file.getAbsolutePath();
					res= uploadFile(app_id, file, null);
					if(!res.contains("file_status=\"Uploaded\"")) {
						System.out.println(file.getName()+" File not Uploaded");
					flag=false;
					}else if((res.contains("file_status=\"Uploaded\""))) {
						flag=true;
					}
					//System.out.println("Files uploaded.");
				}
			}
			System.out.println("Files uploaded.");
		}else{

			// upload multiple files
			for (File file : files) {
				if (file.isFile()) {
					String fileName=file.getAbsolutePath();
					res= uploadFile(app_id, file, sandbox_id);
					if(!res.contains("file_status=\"Uploaded\"")) {
						System.out.println(file.getName()+" File not Uploaded");
						flag=false;
					}
					else if((res.contains("file_status=\"Uploaded\""))) {
						flag=true;
					}

				}
			}
			System.out.println("Files uploaded.");
		}
		return res;

	}
	
	public static String getPreScanResults(String app_id,String build_id,String sandbox_id)
	{
		String url = " https://analysiscenter.veracode.com/api/5.0/getprescanresults.do";
		HashMap<String, String > map= new HashMap<String, String>();
		map.put("app_id", app_id);
		map.put("build_id", build_id);
		map.put("sandbox_id", sandbox_id);
		String response = callURL(url,map,null);

		return response;
	}
	public static String pollPreScan(String app_id,String build_id,String sandbox_id) throws IOException, InterruptedException{
		String res=null;
		if(sandbox_id==null) {
			res = getBuildInfo(app_id, build_id, null);
			System.out.print("Waiting for Pre_Scan Success...");
			//validateResponse(res);
			boolean state=false;
			do{

				state = res.contains("status=\"Pre-Scan Success\"");
				//System.out.println(state);
				if(state == false) {
					//System.out.println(res);
					System.out.print(".");
					Thread.sleep(20000);
					res = getBuildInfo(app_id, build_id, null);
				}
			}while(!state);

		}
		else {

			res= getBuildInfo(app_id, build_id, sandbox_id);
			System.out.print("Waiting for Pre_Scan Success...");
			//validateResponse(res);
			boolean state=false;
			do{

				state = res.contains("status=\"Pre-Scan Success\"");
				//System.out.println(state);
				if(state == false) {
					//System.out.println(res);

					System.out.print(".");
					Thread.sleep(20000);
					res= getBuildInfo(app_id, build_id, sandbox_id);
				}
			}while(!state);

		}
		System.out.println();
		System.out.println("Pre-scan success..");


		return res;

	}
	
	public static String callURL(String myURL, HashMap<String, String> map, File file) {

		//System.out.println("Requeted URL:" + myURL);
		StringBuilder sb = new StringBuilder();
		InputStreamReader in = null;
		String username="User";
		String password="Password";
		String name;
		String value;
		try {
			URL url = new URL(myURL);			
			String userpass = username+":"+password;
			String basicAuth = "Basic " + javax.xml.bind.DatatypeConverter.printBase64Binary(userpass.getBytes());
			urlConn= (HttpURLConnection) url.openConnection();
			urlConn.setRequestMethod("POST");
			urlConn.setRequestProperty("Authorization", basicAuth);

			urlConn.setUseCaches(false);
			urlConn.setDoOutput(true); // indicates POST method
			urlConn.setDoInput(true);
			urlConn.setRequestProperty("Content-Type","multipart/form-data; boundary=" + boundary);
			outputStream=urlConn.getOutputStream();
			//FileInputStream inputStream;
			writer = new PrintWriter(new OutputStreamWriter(outputStream, "UTF-8"), true);

			if(file !=null) {
				addFilePart("file", file);
				for(Entry<String, String > entry : map.entrySet()) {
					name=entry.getKey();
					value=entry.getValue();
					addFormField(name, value);
				}
			} 
			else {
				for(Entry<String, String > entry : map.entrySet()) {
					name=entry.getKey();
					value=entry.getValue();
					addFormField(name, value);
				}
			}

			writer.append(LINE_FEED).flush();
			writer.append("--" + boundary + "--").append(LINE_FEED);
			writer.close();
			if (urlConn != null)
				urlConn.setReadTimeout(60 * 1000);
			if (urlConn != null && urlConn.getInputStream() != null) {
				in = new InputStreamReader(urlConn.getInputStream(),
						Charset.defaultCharset());
				BufferedReader bufferedReader = new BufferedReader(in);
				if (bufferedReader != null) {
					int cp;
					while ((cp = bufferedReader.read()) != -1) {
						sb.append((char) cp);
					}
					bufferedReader.close();
				}
			}
			in.close();
		} catch (Exception e) {
			throw new RuntimeException("Exception while calling URL:"+ myURL, e);
		} 

		return sb.toString();
	}
	/**
	 * Adds a form field to the request
	 *
	 * @param name  field name
	 * @param value field value
	 */
	public  static void addFormField(String name, String value) {
		//System.out.println("addFormField*****************************");
		writer.append("--" + boundary).append(LINE_FEED);
		writer.append("Content-Disposition: form-data; name=\"" + name + "\"")
		.append(LINE_FEED);
		writer.append("Content-Type: text/plain; charset=" + charset).append( LINE_FEED);
		writer.append(LINE_FEED);
		writer.append(value).append(LINE_FEED);
		writer.flush();
	}
	public static void addFilePart(String fieldName, File uploadFile)
			throws IOException {
		//System.out.println("addFilePart************************");
		String fileName = uploadFile.getName();
		writer.append("--" + boundary).append(LINE_FEED);
		writer.append("Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"")
		.append(LINE_FEED);
		writer.append(
				"Content-Type: "
						+ URLConnection.guessContentTypeFromName(fileName))
						.append(LINE_FEED);
		writer.append("Content-Transfer-Encoding: binary").append(LINE_FEED);
		writer.append(LINE_FEED);
		writer.flush();

		FileInputStream inputStream = new FileInputStream(uploadFile);
		byte[] buffer = new byte[4096];
		int bytesRead = -1;
		while ((bytesRead = inputStream.read(buffer)) != -1) {
			outputStream.write(buffer, 0, bytesRead);
		}
		outputStream.flush();
		inputStream.close();

		writer.append(LINE_FEED);
		writer.flush();
	}

	public static boolean validateResponse(String response){
		if (!(response.contains("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))) {
			System.out.println("Response body is not XML format");
			System.exit(1);
			//return false;
		}
		else	if(response.contains("error")){
			String errorMsg = response.substring(response.indexOf("<error>")+7,response.indexOf("</error>"));
			System.out.println(errorMsg);
			//System.exit(1);
			return false;
		}
		return true;

	}
}
