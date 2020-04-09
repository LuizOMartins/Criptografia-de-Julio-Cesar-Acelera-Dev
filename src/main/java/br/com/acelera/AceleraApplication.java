package br.com.acelera;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AceleraApplication {

	private static final String tokenURI = "a74d41671e709cdc208bc2999506613c4935e9a8";
	private static final String POST_URL = "https://api.codenation.dev/v1/challenge/dev-ps/generate-data?token="
			+ tokenURI;
	private static final String POST_SEND = "https://api.codenation.dev/v1/challenge/dev-ps/submit-solution?token="+tokenURI;

	public static void main(String[] args) throws IOException, ParseException {
		SpringApplication.run(AceleraApplication.class, args);

		JSONObject oJSON = RequestGet("https://api.codenation.dev/v1/challenge/dev-ps/generate-data?token=" + tokenURI);
		SaveFileJSON(oJSON);

		// Read a JSON
		Object obj = new JSONParser().parse(new FileReader("answer.json"));
		// typecasting obj to JSONObject
		JSONObject jo = (JSONObject) obj;
		long numero_casas = (long) jo.get("numero_casas");
		String token = (String) jo.get("token");
		String cifrado = (String) jo.get("cifrado");
		String Decifrado = (String) jo.get("decifrado");
		String resumoCriptografico = (String) jo.get("resumo_criptografico");

		Decifrado = decriptar((int) numero_casas, cifrado);
		String sha1 = org.apache.commons.codec.digest.DigestUtils.sha1Hex(Decifrado);

		jo.put("token", tokenURI);
		jo.put("decifrado", Decifrado);
		jo.put("resumo_criptografico", sha1);

		SaveFileJSON(jo);

		sendPOST();

	}

	public static String decriptar(int chave, String textoCifrado) {
		// Variavel que ira guardar o texto decifrado
		StringBuilder texto = new StringBuilder();
		// Descriptografa cada caracter por vez
		for (int c = 0; c < textoCifrado.length(); c++) {
			int charOld = ((int) textoCifrado.charAt(c));
			int letraDecifradaASCII = ((int) textoCifrado.charAt(c)) - chave;

			if (letraDecifradaASCII < 97) {
				int aux = 96 - letraDecifradaASCII;
				aux = (122 - aux);
				letraDecifradaASCII = aux;
			}
			if (charOld < 97 || charOld > 122)
				letraDecifradaASCII = charOld;
			texto.append((char) letraDecifradaASCII);
		}
		return texto.toString();
	}

	private static JSONObject RequestGet(String uri) throws IOException, ClientProtocolException, ParseException {
		try (CloseableHttpClient client = HttpClientBuilder.create().build()) {
			HttpGet request = new HttpGet(uri);
			HttpResponse response = client.execute(request);
			BufferedReader bufReader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			StringBuilder builder = new StringBuilder();
			String line;
			while ((line = bufReader.readLine()) != null) {

				builder.append(line);
				builder.append(System.lineSeparator());
			}
			JSONParser parse = new JSONParser();
			JSONObject jobj = (JSONObject) parse.parse(builder.toString());
			return jobj;
		}
	}

	private static void SaveFileJSON(JSONObject jobj) throws FileNotFoundException {

		PrintWriter pw = new PrintWriter("answer.json");
		pw.write(jobj.toJSONString());
		pw.flush();
		pw.close();
	}

	private static void sendPOST() throws ClientProtocolException, IOException {

		CloseableHttpClient client = HttpClients.createDefault();
		HttpPost httpPost = new HttpPost(POST_SEND);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setCharset(StandardCharsets.UTF_8);
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		builder.addTextBody("file","answer");
		builder.addBinaryBody("answer", new File("answer.json"), ContentType.APPLICATION_OCTET_STREAM, "answer.json");

		HttpEntity multipart = builder.build();
		httpPost.setEntity(multipart);

		CloseableHttpResponse response = client.execute(httpPost);
		
		System.err.println(EntityUtils.toString(response.getEntity(), "UTF-8"));
		System.err.println(response.getEntity());
		client.close();
	}
}
