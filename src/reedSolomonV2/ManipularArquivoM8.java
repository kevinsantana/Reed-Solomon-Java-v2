package reedSolomonV2;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.nio.file.Path;

/*
		TODO: Tratar os casos em que o nome do arquivo contem um ponto no metodo recuperoDiretorioNomeExtensao
		
		TODO: Trocar nomes de variaveis, principalmente aquelas que controlam a copia no System.arraycopy (codificacao e decodificacao)
		
		TODO: Ajustar o metodo corrompeDado para corromper 39 simbolos a cada 255, ate atingir o fim do arquivo
 
        Para m=8:
        Parametro 1 --> Default Primitive Polynomial=D^8 + D^4 + D^3 + D^2 + 1; Integer Representation=285.
        Parametro 2 --> n=2^m = 256
        n=200; k=120; n-k=80; t=40;
        
        RS para m=8:
        Com 5% de erro:
        0.1*255 = 255-k
        k=229 Bytes
        n-k=26 Bytes de redundancia
        
        Com 10% de erro:
        0.2*255 = 255-k
        k=203 Bytes
        n-k=52 Bytes de redundancia
        
        Com 15% de erro:
        0.3*255 = 255-k
        k=177 Bytes
        n-k=78 Bytes de redundancia
 */

public class ManipularArquivoM8 {

	public static void main(String[] args) throws IOException, ReedSolomonException {

		String localAbsoluto = "Z:\\@desenvolvimento\\workspace\\Testes-com-RS-GF(2^16)\\aula11contabcespe.pdf";
		ManipularArquivoM8.degradacaoCorretiva(localAbsoluto);
	}

	private static void degradacaoCorretiva(String localAbsoluto) throws IOException, ReedSolomonException {

		// Com 15% de erro: k=177 Bytes; n-k=78 Bytes de redund�ncia; capacidade de
		// correcao t=39

		// Codificacao
		ManipularArquivoM8.degradacaoCorretivaCodificacao(localAbsoluto);

		// Decodificacao
		ManipularArquivoM8.degradacaoCorretivaDecodificacao(localAbsoluto);
	}

	// Metodo responsavel pela codificacao do RS m = 8, com 15% de correcao
	private static void degradacaoCorretivaCodificacao(String localAbsoluto) throws IOException, ReedSolomonException {

		// Com 15% de erro: k=177 Bytes; n-k=78 Bytes de redund�ncia; quantidade maxima
		// de correcao de t=39 simbolos
		int n = 255, k = 177, t = 39, qtdSimbolosCorrecao = 78;
		int incrementoVetorRS8C = 0, incrementoVetorUnico = 0, incrementoVetorCorrecao = 0;
		Path path = Paths.get(localAbsoluto);
		byte[] dado = Files.readAllBytes(path);
		int qtdSimbolos = dado.length;
		int qtdIteracoesRS = qtdSimbolos / k;
		int restoVetorRS = qtdSimbolos % k;
		int[] arquivo = ManipularArquivoM8.byteSignedParaUnsigned(localAbsoluto);
		int[] vetorRS8Codificado = new int[qtdSimbolos];
		int[] vetorSimbolosCorrecao = new int[((qtdIteracoesRS + 1) * qtdSimbolosCorrecao)];
		int[] vetorRS8C = new int[255];
		int sourcePosResto = qtdSimbolos - restoVetorRS;
		int destPosVetCorrecao = qtdIteracoesRS * qtdSimbolosCorrecao;

		GenericGF gf = new GenericGF(285, 256, 1);
		ReedSolomonEncoder encoder = new ReedSolomonEncoder(gf);

		//System.out.println("Inteiros unsigned ORIGINAIS do arquivo: " + "\n" + Arrays.toString(arquivo) + "\n");

		/*
		 * Rotina de codificacao A ultima iteracao e tratada separadamente, quando ha
		 * resto na divisao qtdSimbolos/k A codificacao e efetuada a cada 255 simbolos
		 * do total de simbolos do arquivo O vetor original e divido em vetores de 255,
		 * ao final da codificacao sao gravados dois arquivos:
		 * Nome_do_arquivo_Codificado e Nome_do_arquivo_Redundancia O arquivo codificado
		 * e o arquivo original corrompido t posicoes, a redundancia sao os simbolos de
		 * correcao gerados a cada iteracao do for, de cada vetor do arquivo original
		 */

		for (int h = 0; h < qtdIteracoesRS; h++) {
			// A cada iteracao o vetorRS8C recebe 177 simbolos do arquivo original
			// As 78 posicoes restantes sao reservadas para geracao dos simbolos de correcao
			// desses 177 simbolos
			System.arraycopy(arquivo, incrementoVetorRS8C, vetorRS8C, 0, k);
			incrementoVetorRS8C += 177;

			// Depois de preenchido, sao gerados os simbolos de correcao do vetorRS8C a
			// partir da codificacao
			encoder.encode(vetorRS8C, qtdSimbolosCorrecao);

			// os k simbolos codificados pelo RS8 a cada iteracao sao armazenados em um
			// univo vetor
			System.arraycopy(vetorRS8C, 0, vetorRS8Codificado, incrementoVetorUnico, k);
			incrementoVetorUnico += 177;

			// os n-k simbolos de correcao codificados pelo RS8 sao armazenados em um unico
			// vetor
			System.arraycopy(vetorRS8C, k, vetorSimbolosCorrecao, incrementoVetorCorrecao, qtdSimbolosCorrecao);
			incrementoVetorCorrecao += 78;

		}
		// Tratamento para o caso em que restoVetorRS > 0 - FUNCIONANDO
		// Crio um vetor adicional apenas quando a resto na divisao 1774/177
		if (restoVetorRS > 0) {
			// Novo vetor apenas para o resto do vetor arquivo
			int[] vetorRS8CResto = new int[255];
			// Copia dos k simbolos de resto do vetor arquivo para o vetorRS8CResto
			System.arraycopy(arquivo, sourcePosResto, vetorRS8CResto, 0, restoVetorRS);
			// Codificacao dos simbolos restantes, a codificacao e feita
			// independentemente do resto, serao sempre codificados 177 simbolos (mesmo os
			// zeros)
			encoder.encode(vetorRS8CResto, qtdSimbolosCorrecao);
			// Copia dos k simbolos para o vetor unico vetorRS8Codificado
			System.arraycopy(vetorRS8CResto, 0, vetorRS8Codificado, sourcePosResto, restoVetorRS);
			// Copia dos simbolos de correcao do resto para o vetorSimbolosCorrecao
			System.arraycopy(vetorRS8CResto, k, vetorSimbolosCorrecao, destPosVetCorrecao, qtdSimbolosCorrecao);
		}

		if (Arrays.equals(arquivo, vetorRS8Codificado) != true) {
			throw new ReedSolomonException(
					"Erro na codificacao, vetor codificado nao e igual ao vetor do arquivo (em inteiro unsigned)");
		} else {
			System.out.println("Arquivo codificado com sucesso! " + "\n");
		}
		

		// Cria vetor de bytes ja codificados pelo encoder e corrompido t posicoes
		byte[] vetorCodificadoSigned = ManipularArquivoM8.byteUnsignedParaSigned(vetorRS8Codificado);
		ManipularArquivoM8.corrompeDado(vetorCodificadoSigned, t);

		// Gravar arquivo codificado e corrompido
		ManipularArquivoM8.gravaArquivoCodificado(vetorCodificadoSigned, localAbsoluto);
		ManipularArquivoM8.gravarRedundancia(vetorSimbolosCorrecao, localAbsoluto);
	}

	// Metodo responsavel pela decodificacao do RS m = 8, com 15% de correcao
	private static void degradacaoCorretivaDecodificacao(String localAbsoluto)
			throws IOException, ReedSolomonException {

		// Com 15% de erro: k=177 Bytes n-k=78 Bytes de redund�ncia t=39
		int n = 255, k = 177, t = 39, qtdSimbolosCorrecao = 78, destPos = 0;
		int srcPosDecodi = 0, incrementoVetorDecodificado = 0;
		Path path = Paths.get(localAbsoluto);
		byte[] dado = Files.readAllBytes(path);
		int qtdSimbolos = dado.length;
		int qtdIteracoesRS = qtdSimbolos / k;
		int restoVetorRS = qtdSimbolos % k;
		int[] vetorRS8D = new int[255];
		int destPosRS8D = vetorRS8D.length - qtdSimbolosCorrecao;
		int incrementoVetorResto = qtdSimbolos - restoVetorRS;
		int sourcePosRestoD = qtdIteracoesRS * qtdSimbolosCorrecao;
		int[] arquivo = ManipularArquivoM8.byteSignedParaUnsigned(localAbsoluto);
		int[] vetorRS8Decodificado = new int[qtdSimbolos];

		GenericGF gf = new GenericGF(285, 256, 1);
		ReedSolomonDecoder decoder = new ReedSolomonDecoder(gf);

		// Recupera arquivo codificado e redundancia
		int[] vetorSimbolosCorrecao = new int[((qtdIteracoesRS + 1) * qtdSimbolosCorrecao)];
		vetorSimbolosCorrecao = recuperaRedundanciaGravada(localAbsoluto);
		int[] voltaCodificado = ManipularArquivoM8.byteSignedParaUnsignedSemParidade(localAbsoluto);

		// Rotina de decodificacao
		for (int h = 0; h < qtdIteracoesRS; h++) {

			// Quebrar vetor unico de simbolos codificados em vetores de 255
			// Copia 177 simbolos de informacao para o vetorRS8D
			System.arraycopy(voltaCodificado, incrementoVetorDecodificado, vetorRS8D, 0, k);
			incrementoVetorDecodificado += 177;

			// Concatenando dado com redudancia e efetua Decodificacao
			System.arraycopy(vetorSimbolosCorrecao, destPos, vetorRS8D, destPosRS8D, qtdSimbolosCorrecao);
			destPos += 78;

			// Decodificacao
			decoder.decode(vetorRS8D, qtdSimbolosCorrecao);

			// Guardar o que foi decodificado em um unico vetor para gravar o arquivo
			// decodificado
			System.arraycopy(vetorRS8D, 0, vetorRS8Decodificado, srcPosDecodi, k);
			srcPosDecodi += 177;
		}
		// Tratamento quando ha resto na divisao qtdSimbolosArquivo / k
		if (restoVetorRS > 0) {
			// Vetor para tratamento do resto
			int[] vetorRS8DResto = new int[255];
			// Copia k simbolos de resto para vetorRS8DResto
			System.arraycopy(voltaCodificado, incrementoVetorResto, vetorRS8DResto, 0, restoVetorRS);
			// Copia n-k simbolos de correcao para vetorRS8DResto
			System.arraycopy(vetorSimbolosCorrecao, sourcePosRestoD, vetorRS8DResto, k, qtdSimbolosCorrecao);
			// Decodificacao do resto
			decoder.decode(vetorRS8DResto, qtdSimbolosCorrecao);
			// Copia dos k simbolos de resto para vetor unico
			System.arraycopy(vetorRS8DResto, 0, vetorRS8Decodificado, incrementoVetorResto, restoVetorRS);
			// Copia dos n-k simbolos de correcao do resto para vetor unico
			System.arraycopy(vetorRS8DResto, k, vetorSimbolosCorrecao, sourcePosRestoD, qtdSimbolosCorrecao);
		}

		if (Arrays.equals(arquivo, vetorRS8Decodificado) != true) {
			throw new ReedSolomonException(
					"Erro na decodificacao, vetor decodificado nao e igual ao vetor do arquivo (em inteiro unsigned)");
		} else {
			System.out.println("Arquivo decodificado com sucesso! ");
		}

		// Gravacao em disco do arquivo decodificado e corrigido
		byte[] decodificado = ManipularArquivoM8.byteUnsignedParaSigned(vetorRS8Decodificado);
		ManipularArquivoM8.gravaArquivoDecodificado(decodificado, localAbsoluto);
	}

	/*
	 * Transformar os bytes de um arquivo lido de SIGNED [-128 a 127] em UNSIGNED [0
	 * a 255] e escrever em um vetor de inteiros e acrescentar n-k posicoes no vetor
	 * devolvido, pois, o bloco n do RS e composto por k(informacao) e n-k(paridade)
	 * Usar APENAS na primeira leitura do arquivo para vetor de bytes
	 */
	private static int[] byteSignedParaUnsigned(String fileName) throws IOException {
		Path path = Paths.get(fileName);
		byte[] bytesArquivoLido = Files.readAllBytes(path);
		int tamanhoVetorInt = bytesArquivoLido.length;
		int valorEmIntDoByte = 0;
		int[] vetorDevolvido = new int[tamanhoVetorInt];

		// Bytes em java representam inteiros em complemento de dois, a soma e feita nos
		// bytes com valores negativos
		// Pois, o RS opera com numeros inteiros e positivos
		for (int i = 0; i < tamanhoVetorInt; i++) {
			valorEmIntDoByte = bytesArquivoLido[i];
			if (valorEmIntDoByte < 0) {
				valorEmIntDoByte = valorEmIntDoByte + 256;
			}
			vetorDevolvido[i] = valorEmIntDoByte;
		}
		return vetorDevolvido;
	}

	// Transformar os bytes de um arquivo lido de SIGNED [-128 a 127] em UNSIGNED [0
	// a 255] e escrever em um vetor de inteiros
	// Usar esse metodos para as demais conversoes que nao a primeira
	static int[] byteSignedParaUnsignedSemParidade(String localAbsoluto) throws IOException {

		String[] diretorioArquivoExtensao = recuperoDiretorioNomeExtensao(localAbsoluto);
		String diretorio = diretorioArquivoExtensao[0];
		String arquivo = diretorioArquivoExtensao[1];
		String extensao = diretorioArquivoExtensao[2];
		String arquivoCompleto = diretorio + arquivo + "_" + "Codificado" + "." + extensao;

		Path path = Paths.get(arquivoCompleto);
		byte[] bytesArquivoLido = Files.readAllBytes(path);
		int tamanhoVetorInt = bytesArquivoLido.length;
		int valorEmIntDoByte = 0;
		int[] vetorDevolvido = new int[tamanhoVetorInt];

		// Bytes em java representam inteiros em complemento de dois, a soma e feita nos
		// bytes com valores negativos
		// Pois, o RS opera com numeros inteiros e positivos
		for (int i = 0; i < tamanhoVetorInt; i++) {
			valorEmIntDoByte = bytesArquivoLido[i];
			if (valorEmIntDoByte < 0) {
				valorEmIntDoByte = valorEmIntDoByte + 256;
			}
			vetorDevolvido[i] = valorEmIntDoByte;
		}
		return vetorDevolvido;
	}

	// Metodo para conversao de int[] para byte[] apenas em inteiros com sinal e
	// oito bits (complemento de 2 do Java)
	// Metodo para transformar um byte UNSIGNED em SIGNED
	private static byte[] byteUnsignedParaSigned(int[] vetorIntUnsigned) throws IOException {
		int valorEmIntDoByte = 0;
		int tamanho = vetorIntUnsigned.length;
		byte[] novoVetorBytes = new byte[tamanho];
		for (int i = 0; i < novoVetorBytes.length; ++i) {
			valorEmIntDoByte = vetorIntUnsigned[i];
			novoVetorBytes[i] = byteParaInt(valorEmIntDoByte);
		}
		return novoVetorBytes;
	}

	// Metodo auxiliar para transformar um unico inteiro unsigned em inteiro signed
	// apenas em inteiro
	// com sinal e oito bits (complemento de 2 do Java)
	private static byte byteParaInt(int intByte) {
		byte valorDoByteEmInt = 0;
		if (intByte <= 256) {
			valorDoByteEmInt = (byte) intByte;
			if (valorDoByteEmInt > 127) {
				valorDoByteEmInt = (byte) (valorDoByteEmInt - 256);
			}
		}
		return valorDoByteEmInt;
	}

	/*
	 * Recupera nome e local de um arquivo a partir do local absoluto // Entrada:
	 * string representando o local absoluto // Retorno: Um vetor de strings, cuja
	 * posicao [0] eh o local onde este arquivo // esta gravado no disco // [1] eh o
	 * nome do arquivo e a posicao [2] eh a extensao do arquivo sem ponto //
	 * Recuperar local independente de quantas pastas existam 
	 */
	private static String[] recuperoDiretorioNomeExtensao(String localAbsoluto) {
		File f = new File(localAbsoluto);

		// Recupera nome e extensao do arquivo
		String arquivoComExtensao = "";
		int in = f.getAbsolutePath().lastIndexOf("\\");
		if (in > -1) {
			arquivoComExtensao = f.getAbsolutePath().substring(in + 1);
		}
		// Recupera o diretorio onde o arquivo esta armazenado
		String diretorio = localAbsoluto.replace(arquivoComExtensao, "");

		// Recupera nome e extensao do arquivo
		String[] separaNomeDaExtensao = arquivoComExtensao.split("[.]");
		String arquivo = separaNomeDaExtensao[0];
		String extensao = separaNomeDaExtensao[1];

		// Armazena diretorio, nome do arquivo e extensao do arquivo em um array de
		// strings
		String[] diretorioArquivoExtensao = new String[3];
		diretorioArquivoExtensao[0] = diretorio;
		diretorioArquivoExtensao[1] = arquivo;
		diretorioArquivoExtensao[2] = extensao;

		return diretorioArquivoExtensao;
	}

	// Gravar codificado
	private static File gravaArquivoCodificado(byte[] arquivoCodificado, String localAbsoluto) throws IOException {

		String[] diretorioArquivoExtensao = recuperoDiretorioNomeExtensao(localAbsoluto);
		String diretorio = diretorioArquivoExtensao[0];
		String arquivo = diretorioArquivoExtensao[1];
		String extensao = diretorioArquivoExtensao[2];
		String arquivoCompleto = diretorio + arquivo + "_" + "Codificado" + "." + extensao;

		// Grava o arquivo com o nome fornecido e a extensao lida
		File newFile = new File(arquivoCompleto);
		FileOutputStream stream = new FileOutputStream(newFile);
		stream.write(arquivoCodificado);
		stream.close();
		return newFile;
	}

	// Gravar decodificado
	private static File gravaArquivoDecodificado(byte[] arquivoDecodificado, String localAbsoluto) throws IOException {

		String[] diretorioArquivoExtensao = recuperoDiretorioNomeExtensao(localAbsoluto);
		String diretorio = diretorioArquivoExtensao[0];
		String arquivo = diretorioArquivoExtensao[1];
		String extensao = diretorioArquivoExtensao[2];
		String arquivoCompleto = diretorio + arquivo + "_" + "Decodificado" + "." + extensao;

		// Grava o arquivo com o nome fornecido e a extensao lida
		File newFile = new File(arquivoCompleto);
		FileOutputStream stream = new FileOutputStream(newFile);
		stream.write(arquivoDecodificado);
		stream.close();
		return newFile;
	}

	// Gravar a redundancia em um arquivo
	private static void gravarRedundancia(int[] vetorRedundancia, String localAbsoluto) throws IOException {

		String[] diretorioArquivoExtensao = recuperoDiretorioNomeExtensao(localAbsoluto);
		String diretorio = diretorioArquivoExtensao[0];
		String arquivo = diretorioArquivoExtensao[1];
		String extensao = diretorioArquivoExtensao[2];
		String arquivoCompleto = diretorio + arquivo + "_" + "Redundancia" + "." + extensao;

		// Grava o arquivo
		OutputStream os = new FileOutputStream(arquivoCompleto);
		for (int i = 0; i < vetorRedundancia.length; i++) {
			os.write(vetorRedundancia[i]);
		}
		os.close();
	}
	/*
	// Corromper vetor de dados t posicoes, 
	// TODO: trocar o for por system.arraycopy
	private static void corrompeDado(byte[] dados, int t) {
		// Vetor de bytes com SecureRandom de tamanho t
		SecureRandom random = new SecureRandom();
		byte[] corrupcao = new byte[t];
		random.nextBytes(corrupcao);
		// Concatenando dado com corrupcao randomica, de acordo com t
		int l = 0;
		for (int x = 0; x < t; x++) {
			dados[x] = corrupcao[l];
			l++;
		}
	}*/
	
	// Corrompe 15% do arquivo com bytes randomicos
	private static void corrompeDado(byte[] dados, int t) {
		int qtdIteracoes = dados.length / 177;
		int resto = dados.length % 177;
		int incrementoVetorDados = 0;
		SecureRandom random = new SecureRandom();		
		
		for (int x = 0; x < qtdIteracoes; x++) {
			byte[] corrupcao = new byte[t];
			random.nextBytes(corrupcao);
			//System.out.println(Arrays.toString(corrupcao));
			System.arraycopy(corrupcao, 0, dados, incrementoVetorDados, t);
			incrementoVetorDados =+ 255; 
		}
		if (resto > 0) {
			byte[] corrupcao = new byte[t];
			random.nextBytes(corrupcao);
			System.arraycopy(corrupcao, 0, dados, dados.length - resto, t);
		}
	}

	private static int[] recuperaRedundanciaGravada(String localAbsoluto) throws IOException {

		String[] diretorioArquivoExtensao = recuperoDiretorioNomeExtensao(localAbsoluto);
		String diretorio = diretorioArquivoExtensao[0];
		String arquivo = diretorioArquivoExtensao[1];
		String extensao = diretorioArquivoExtensao[2];
		String arquivoCompleto = diretorio + arquivo + "_" + "Redundancia" + "." + extensao;

		Path path = Paths.get(arquivoCompleto);
		byte[] bytesRedundancia = Files.readAllBytes(path);
		int tamanhoVetorInt = bytesRedundancia.length;
		int valorEmIntDoByte = 0;
		int[] vetorDevolvido = new int[tamanhoVetorInt];

		// Bytes em java representam inteiros em complemento de dois, a soma e feita nos
		// bytes com valores negativos
		// Pois, o RS opera com numeros inteiros e positivos
		for (int i = 0; i < tamanhoVetorInt; i++) {
			valorEmIntDoByte = bytesRedundancia[i];
			if (valorEmIntDoByte < 0) {
				valorEmIntDoByte = valorEmIntDoByte + 256;
			}
			vetorDevolvido[i] = valorEmIntDoByte;
		}
		return vetorDevolvido;
	}
}