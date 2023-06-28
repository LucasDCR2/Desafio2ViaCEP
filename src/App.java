import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class App {
    private static final String VIA_CEP_ENDERECO_API_URL = "http://viacep.com.br/ws/%s/%s/%s/json/";
    private static final String VIA_CEP_CEP_API_URL = "http://viacep.com.br/ws/%s/json/";

    public static void main(String[] args) {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean sair = false;

            while (!sair) {
                System.out.println("Escolha uma opcao:");
                System.out.println("1. Buscar por endereco");
                System.out.println("2. Buscar por CEP");
                System.out.println("3. Sair");
                System.out.print("Opcao: ");
                String opcao = scanner.nextLine();

                switch (opcao) {
                    case "1":
                        System.out.print("Informe o UF: ");
                        String uf = scanner.nextLine();
                        System.out.print("Informe a cidade: ");
                        String cidade = scanner.nextLine();
                        System.out.print("Informe o logradouro: ");
                        String logradouro = scanner.nextLine();
                        System.out.println();

                        Map<String, String[]> resultados = consultarEndereco(uf, cidade, logradouro);
                        exibirResultadosEndereco(resultados);
                        break;
                    case "2":
                        System.out.print("Informe o CEP: ");
                        String cep = scanner.nextLine();
                        System.out.println();

                        Map<String, String[]> resultado = consultarCep(cep);
                        exibirResultadoCep(resultado);
                        System.out.println();
                        break;
                    case "3":
                        System.out.println("Saindo...");
                        sair = true; // Define a flag para sair do loop
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
        texto = Normalizer.normalize(texto, Normalizer.Form.NFD);
        texto = texto.replaceAll("[^\\p{ASCII}]", "");
        return texto;
    }

    private static void exibirResultadosEndereco(Map<String, String[]> resultados) {
        if (resultados.isEmpty()) {
            System.out.println("Nenhum resultado encontrado.");
        } else {
            System.out.println("CEP                     Bairro                  Logradouro");
            System.out.println("----------------------------------------------------------");

            for (Map.Entry<String, String[]> entry : resultados.entrySet()) {
                String cep = entry.getKey();
                String[] dados = entry.getValue();

                String bairro = dados[0];
                String logradouro = dados[1];

                System.out.printf("%-24s%-24s%-24s%n", cep, bairro, logradouro);
            }
        }

        System.out.println();
    }

    private static void exibirResultadoCep(Map<String, String[]> resultado) {
        if (resultado == null) {
            System.out.println("Nenhum resultado encontrado.");
        } else {
            for (Map.Entry<String, String[]> entry : resultado.entrySet()) {
                String cep = entry.getKey();
                String[] dados = entry.getValue();

                String logradouro = dados[0];
                String complemento = dados[1];
                String bairro = dados[2];
                String localidade = dados[3];
                String uf = dados[4];
                String ddd = dados[5];

                System.out.println("CEP: " + cep);
                System.out.println("Logradouro: " + logradouro);
                System.out.println("Complemento: " + complemento);
                System.out.println("Bairro: " + bairro);
                System.out.println("Localidade: " + localidade);
                System.out.println("UF: " + uf);
                System.out.println("DDD: " + ddd);
            }
        }
    }
}
