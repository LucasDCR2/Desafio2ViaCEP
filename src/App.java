import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class App {

    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            Logica logica = new Logica();

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
                        logica.buscarPorEndereco(reader);
                        break;
                    case "2":
                        logica.buscarPorCep(reader);
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
}
