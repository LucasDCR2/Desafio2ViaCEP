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

    private static Map<String, Map<String, String[]>> cache = new HashMap<>();

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            boolean sair = false;
            while (!sair) {
                System.out.println("Escolha uma opcao:");
                System.out.println("1. Buscar por endereco");
                System.out.println("2. Buscar por CEP");
                System.out.println("3. Sair");
                System.out.print("Opcao: ");
                String opcao = reader.readLine();

                switch (opcao) {
                    case "1":
                        buscarPorEndereco(reader);
                        break;
                    case "2":
                        buscarPorCep(reader);
                        break;
                    case "3":
                        sair = true;
                        break;
                    default:
                        System.out.println("Opção inválida.");
                        break;
                }
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
        System.out.println();

        String cacheKey = uf + cidade + logradouro;
        if (cache.containsKey(cacheKey)) {
            exibirResultadosEndereco(cache.get(cacheKey));
        } else {
            Map<String, String[]> resultados = consultarEndereco(uf, cidade, logradouro);
            cache.put(cacheKey, resultados);
            exibirResultadosEndereco(resultados);
        }

        System.out.println();
    }

    private static void buscarPorCep(BufferedReader reader) throws IOException {
        System.out.print("Informe o CEP: ");
        String cep = reader.readLine();
        System.out.println();

        String cacheKey = cep;
        if (cache.containsKey(cacheKey)) {
            exibirResultadoCep(cache.get(cacheKey));
        } else {
            Map<String, String[]> resultado = consultarCep(cep);
            cache.put(cacheKey, resultado);
            exibirResultadoCep(resultado);
        }

        System.out.println();
    }

    private static Map<String, String[]> consultarEndereco(String uf, String cidade, String logradouro) throws IOException {
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

            return extrairDadosJsonEndereco(response.toString());
        } else {
            System.out.println("Erro na consulta. Código de resposta: " + responseCode);
        }

        return null;
    }

    private static Map<String, String[]> consultarCep(String cep) throws IOException {
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

            return extrairDadosJsonCep(response.toString());
        } else {
            System.out.println("Erro na consulta. Código de resposta: " + responseCode);
        }

        return null;
    }

    private static Map<String, String[]> extrairDadosJsonEndereco(String json) {
        Map<String, String[]> dados = new HashMap<>();

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
                dados.put(cep, new String[]{bairro, logradouro});
            }
        }

        return dados;
    }

    private static Map<String, String[]> extrairDadosJsonCep(String json) {
        Map<String, String[]> dados = new HashMap<>();

        String[] campos = json.replaceAll("[{}\"]", "").split(",");

        String cep = null;
        String logradouro = null;
        String complemento = null;
        String bairro = null;
        String localidade = null;
        String uf = null;
        String ddd = null;

        for (String campo : campos) {
            String[] keyValue = campo.split(":");
            String chave = keyValue[0].trim();
            String valor = keyValue[1].trim();

            if (chave.equals("cep")) {
                cep = formatarTexto(valor);
            } else if (chave.equals("logradouro")) {
                logradouro = formatarTexto(valor);
            } else if (chave.equals("complemento")) {
                complemento = formatarTexto(valor);
            } else if (chave.equals("bairro")) {
                bairro = formatarTexto(valor);
            } else if (chave.equals("localidade")) {
                localidade = formatarTexto(valor);
            } else if (chave.equals("uf")) {
                uf = formatarTexto(valor);
            } else if (chave.equals("ddd")) {
                ddd = formatarTexto(valor);
            }
        }

        if (cep != null) {
            dados.put(cep, new String[]{logradouro, complemento, bairro, localidade, uf, ddd});
        }

        return dados;
    }

    private static String formatarTexto(String texto) {
        return Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "");
    }


    private static void exibirResultadosEndereco(Map<String, String[]> resultados) {
        if (resultados.isEmpty()) {
            System.out.println("Nenhum resultado encontrado.");
        } else {
            System.out.println(String.format("%-15s%-20s%-30s", "CEP", "BAIRRO", "LOGRADOURO"));
            for (Map.Entry<String, String[]> entry : resultados.entrySet()) {
                String cep = entry.getKey();
                String[] dados = entry.getValue();

                String formattedOutput = String.format("%-15s%-20s%-30s", cep, dados[0], dados[1]);
                System.out.println(formattedOutput);
            }
        }
    }


    private static void exibirResultadoCep(Map<String, String[]> resultado) {
        if (resultado.isEmpty()) {
            System.out.println("Nenhum resultado encontrado.");
        } else {
            for (Map.Entry<String, String[]> entry : resultado.entrySet()) {
                String cep = entry.getKey();
                String[] dados = entry.getValue();

                System.out.println("CEP: " + cep);
                System.out.println("Logradouro: " + dados[0]);
                System.out.println("Complemento: " + dados[1]);
                System.out.println("Bairro: " + dados[2]);
                System.out.println("Localidade: " + dados[3]);
                System.out.println("UF: " + dados[4]);
                System.out.println("DDD: " + dados[5]);
                System.out.println();
            }
        }
    }
}
