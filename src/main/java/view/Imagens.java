package view;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

// Imagens de src/main/resources/imagens (no classpath, em /imagens), guardadas por nome (por exemplo, "cartela-tolui").
class Imagens {
    private static final Map<String, BufferedImage> cache = new HashMap<>();

    static BufferedImage tabuleiro() {
        return ler("tabuleiro", 1);
    }

    // As cartelas têm cerca de 6000 x 3200 pixels. Lidas com 1 a cada 4 pixels, ocupam 16 vezes menos memória.
    static BufferedImage cartela(String herdeiro) {
        return ler("cartela-" + herdeiro.toLowerCase(), 4);
    }

    static BufferedImage peao(String herdeiro) {
        return ler("peao-" + herdeiro.toLowerCase(), 1);
    }

    // Peça de tributo: "yurt", "espada" ou "moeda" (cartoes6 do material do jogo).
    static BufferedImage tributo(String tipo) {
        return ler("tributo-" + tipo, 2);
    }

    // Peça de tesouro: "boi", "seda", "ferro", "pele" ou "especiaria" (cartoes7 do material do jogo).
    static BufferedImage tesouro(String tipo) {
        return ler("tesouro-" + tipo, 2);
    }

    // Peça de cidade com o nome da cidade (cartoes2 do material do jogo). O arquivo usa o nome sem acento.
    static BufferedImage cidade(String nome) {
        String arquivo = java.text.Normalizer.normalize(nome.toLowerCase(), java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return ler("cidade-" + arquivo, 2);
    }

    // Carta de conselheiro aberto (cartas1 a cartas3 do material do jogo). As imagens grandes são lidas com 1 a cada
    // 4 pixels; as de cerca de 400 pixels, inteiras.
    static BufferedImage carta(String id) {
        return ler("carta-" + id, id.startsWith("cidades") ? 1 : 4);
    }

    // Peça de ativação do herdeiro, colocada sobre a coluna usada da cartela (cartoes4 do material do jogo).
    static BufferedImage ativacao(String herdeiro) {
        return ler("ativacao-" + herdeiro.toLowerCase(), 1);
    }

    // Cor de cada herdeiro, a mesma do símbolo do seu pião. Jochi usa um cinza alaranjado, que aparece melhor no tabuleiro.
    static Color cor(String herdeiro) {
        return switch (herdeiro) {
            case "Altani" -> new Color(46, 139, 60);
            case "Jochi" -> new Color(176, 128, 88);
            case "Chagatai" -> new Color(200, 40, 40);
            case "Tolui" -> new Color(225, 175, 20);
            default -> new Color(40, 80, 190); // Ogedei
        };
    }

    private static BufferedImage ler(String nome, int passo) {
        BufferedImage imagem = cache.get(nome);
        if (imagem == null) {
            imagem = lerArquivo(nome, passo);
            cache.put(nome, imagem);
        }
        return imagem;
    }

    private static BufferedImage lerArquivo(String nome, int passo) {
        InputStream arquivo = Imagens.class.getResourceAsStream("/imagens/" + nome + ".jpg");
        if (arquivo == null)
            throw new IllegalStateException("Erro em Imagens.lerArquivo: a imagem " + nome + " não foi encontrada.");
        try (arquivo; ImageInputStream in = ImageIO.createImageInputStream(arquivo)) {
            ImageReader leitor = ImageIO.getImageReaders(in).next();
            leitor.setInput(in);
            ImageReadParam param = leitor.getDefaultReadParam();
            param.setSourceSubsampling(passo, passo, 0, 0);
            BufferedImage imagem = leitor.read(0, param);
            leitor.dispose();
            return imagem;
        } catch (IOException e) {
            throw new IllegalStateException("Erro em Imagens.lerArquivo: não foi possível ler a imagem " + nome
                    + " (" + e.getMessage() + ").");
        }
    }
}
