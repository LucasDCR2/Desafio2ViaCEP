import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;

public class App {

    private static final String VIA_CEP_ENDERECO_API_URL = "http://viacep.com.br/ws/%s/%s/%s/json/";
    private static final String VIA_CEP_CEP_API_URL = "http://viacep.com.br/ws/%s/json/";

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            System.out.println("Escolha uma opcao:");
            System.out.println("1. Buscar por endereco");
            System.out.println("2. Buscar por CEP");
            System.out.print("Opcao: ");
            String opcao = reader.readLine();

            if (opcao.equals("1")) {
                buscarPorEndereco(reader);
            } else if (opcao.equals("2")) {
                buscarPorCep(reader);
            } else {
                System.out.println("Opção inválida.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void buscarPorEndereco(BufferedReader reader) throws IOException {
        System.out.print("Informe a UF: ");
        String uf = reader.readLine();
        System.out.print("Informe a cidade: ");
        String cidade = reader.readLine();
        System.out.print("Informe o logradouro: ");
        String logradouro = reader.readLine();

        Map<String, String> resultados = consultarEndereco(uf, cidade, logradouro);
        exibirResultados(resultados);
    }

    private static void buscarPorCep(BufferedReader reader) throws IOException {
        System.out.print("Informe o CEP: ");
        String cep = reader.readLine();

        Map<String, String> resultado = consultarCep(cep);
        exibirResultadoCep(resultado);
    }

    private static Map<String, String> consultarEndereco(String uf, String cidade, String logradouro) throws IOException {
        cidade = cidade.replace(" ", "%20");
        logradouro = logradouro.replace(" ", "%20");

        String urlStr = String.format(VIA_CEP_ENDERECO_API_URL, uf, cidade, logradouro);
        URL url = new URL(urlStr);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();

        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return extrairDadosJson(response.toString());
        } else {
            System.out.println("Erro na consulta. Código de resposta: " + responseCode);
        }

        return null;
    }

    private static Map<String, String> consultarCep(String cep) throws IOException {
        cep = cep.replace("-", "");

        String urlStr = String.format(VIA_CEP_CEP_API_URL, cep);
        URL url = new URL(urlStr);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();

        if (responseCode == 200) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            return extrairDadosJson(response.toString());
        } else {
            System.out.println("Erro na consulta. Código de resposta: " + responseCode);
        }

        return null;
    }

    private static Map<String, String> extrairDadosJson(String json) {
        Map<String, String> dados = new HashMap<>();

        String[] resultados = json.split("},");
        for (String resultado : resultados) {
            resultado = resultado.replaceAll("[{}\"]", "").trim();

            String[] campos = resultado.split(",");

            String cep = null;
            String bairro = null;
            String logradouro = null;

            for (String campo : campos) {
                String[] keyValue = campo.split(":");
                String chave = keyValue[0].trim();
                String valor = keyValue[1].trim();

                if (chave.equals("cep")) {
                    cep = formatarTexto(valor);
                } else if (chave.equals("bairro")) {
                    bairro = formatarTexto(valor);
                } else if (chave.equals("logradouro")) {
                    logradouro = formatarTexto(valor);
                }
            }

            if (cep != null && bairro != null && logradouro != null) {
                dados.put(cep, bairro + ", " + logradouro);
            }
        }

        return dados;
    }

    private static String formatarTexto(String texto) {
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("[^\\p{ASCII}]", "");
        return texto;
    }

    private static void exibirResultados(Map<String, String> resultados) {
        if (resultados != null && !resultados.isEmpty()) {
            System.out.println("CEP\t\t\tBairro\t\t\tLogradouro");
            for (Map.Entry<String, String> entry : resultados.entrySet()) {
                String cep = entry.getKey();
                String endereco = entry.getValue();

                String[] partes = endereco.split(",", 2);
                String bairro = partes[0].trim();
                String logradouro = partes[1].trim();

                System.out.printf("%-15s%-20s%s%n", cep, bairro, logradouro);
            }
        } else {
            System.out.println("Nenhum resultado encontrado.");
        }
    }

    private static void exibirResultadoCep(Map<String, String> resultado) {
        if (resultado != null && !resultado.isEmpty()) {
            System.out.println("CEP\t\t\tBairro\t\t\tLogradouro");
            for (Map.Entry<String, String> entry : resultado.entrySet()) {
                String cep = entry.getKey();
                String endereco = entry.getValue();

                String[] partes = endereco.split(",", 2);
                String bairro = partes[0].trim();
                String logradouro = partes[1].trim();

                System.out.printf("%-15s%-20s%s%n", cep, bairro, logradouro);
            }
        } else {
            System.out.println("Nenhum resultado encontrado.");
        }
    }
}
