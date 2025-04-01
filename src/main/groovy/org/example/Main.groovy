package org.example

import org.jsoup.Jsoup
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.apache.poi.ss.usermodel.*
import java.nio.file.*
import java.net.*
import java.io.*

class TISSCrawler {
    static final String BASE_URL = "https://www.gov.br/ans/pt-br"
    static final String DOWNLOAD_PATH = "/home/gb/Downloads/Arquivos_padrao_TISS"

    static void main(String[] args) {
        new File(DOWNLOAD_PATH).mkdirs()
        println "Pasta criada em: ${new File(DOWNLOAD_PATH).getAbsolutePath()}"

        baixarComponenteComunicacao()
        coletarHistoricoVersoes()
        baixarTabelaErros()
        baixarXLSX()  // Função para baixar o arquivo XLSX
        baixarArquivosAuxiliares()  // Função para baixar o arquivo ZIP dos arquivos auxiliares
    }

    static void baixarComponenteComunicacao() {
        def url = "https://www.gov.br/ans/pt-br/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-2013-tiss/marco-2024"
        try {
            def doc = Jsoup.connect(url).get()
            def link = obterLinkCompleto(doc.select("a:contains(Componente de Comunicação)").attr("href"))
            if (link) {
                // Salvar como .zip
                baixarArquivo(link, "Componente_Comunicacao.zip")
            } else {
                println "Link para Componente de Comunicação não encontrado."
            }
        } catch (Exception e) {
            println "Erro ao buscar Componente de Comunicação: ${e.message}"
        }
    }

    static void coletarHistoricoVersoes() {
        def url = "https://www.gov.br/ans/pt-br/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-2013-tiss/padrao-tiss-historico-das-versoes-dos-componentes-do-padrao-tiss"
        try {
            def doc = Jsoup.connect(url).get()
            def tabela = doc.select("table").first() // Seleciona a primeira tabela da página
            if (tabela) {
                def linhas = tabela.select("tr").drop(1)  // Ignora o cabeçalho da tabela
                def dados = []

                // Itera sobre as linhas da tabela
                linhas.each { linha ->
                    def colunas = linha.select("td").collect { it.text() } // Coleta os textos das células

                    // Verifica se a competência é a partir de jan/2016
                    if (colunas.size() >= 3 && colunas[0].matches(/\d{2}\/\d{4}/)) {
                        if (colunas[0].compareTo("01/2016") >= 0) {
                            // Adiciona competência, publicação e início de vigência
                            dados << [colunas[0], colunas[1], colunas[2]]
                        }
                    }
                }

                // Verifique se há dados coletados antes de salvar
                if (dados.size() > 0) {
                    // Salvar os dados coletados como um arquivo .txt
                    salvarComoTxt(dados, "$DOWNLOAD_PATH/historico_versoes.txt")
                    println "Histórico de versões salvo com sucesso em .txt."
                } else {
                    println "Nenhum dado coletado para o histórico de versões."
                }
            } else {
                println "Tabela de histórico não encontrada."
            }
        } catch (Exception e) {
            println "Erro ao coletar histórico de versões: ${e.message}"
        }
    }

    static void salvarComoTxt(def dados, String caminhoArquivo) {
        try {
            FileWriter writer = new FileWriter(caminhoArquivo)
            BufferedWriter bufferedWriter = new BufferedWriter(writer)

            // Verifica se há dados a serem escritos
            if (dados.isEmpty()) {
                println "Nenhum dado para escrever no arquivo."
                return
            }

            // Escreve os dados no arquivo de texto
            dados.each { linha ->
                bufferedWriter.write("Competência: ${linha[0]}, Publicação: ${linha[1]}, Início de Vigência: ${linha[2]}")
                bufferedWriter.newLine()
            }

            bufferedWriter.close()
            writer.close()
            println "Arquivo salvo com sucesso em: $caminhoArquivo"
        } catch (Exception e) {
            println "Erro ao salvar arquivo como .txt: ${e.message}"
        }
    }

    static void baixarTabelaErros() {
        def url = "https://www.gov.br/ans/pt-br/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-2013-tiss/padrao-tiss-tabelas-relacionadas"
        try {
            def doc = Jsoup.connect(url).get()
            def link = obterLinkCompleto(doc.select("a:contains(Tabela de erros)").attr("href"))
            if (link) {
                baixarArquivo(link, "Tabela_Erros.zip") // Salvar como .zip
            } else {
                println "Link para a Tabela de Erros não encontrado."
            }
        } catch (Exception e) {
            println "Erro ao buscar Tabela de Erros: ${e.message}"
        }
    }

    static void baixarXLSX() {
        // Substitua pela URL correta do arquivo XLSX
        def urlXLSX = "https://www.gov.br/ans/pt-br/arquivos/assuntos/prestadores/padrao-para-troca-de-informacao-de-saude-suplementar-tiss/padrao-tiss-tabelas-relacionadas/Tabelaerrosenvioparaanspadraotiss__1_.xlsx"
        try {
            if (urlXLSX) {
                baixarArquivo(urlXLSX, "Arquivo_XLSX.xlsx")
                println "Arquivo XLSX baixado com sucesso."
            } else {
                println "Link do arquivo XLSX não encontrado."
            }
        } catch (Exception e) {
            println "Erro ao baixar arquivo XLSX: ${e.message}"
        }
    }

    static void baixarArquivosAuxiliares() {
        def url = "https://www.ans.gov.br/arquivos/extras/tiss/Padrao_TISS_arquivos_auxiliares_202403.zip"
        try {
            if (url) {
                baixarArquivo(url, "Padrao_TISS_arquivos_auxiliares_202403.zip")
                println "Arquivo auxiliar baixado com sucesso."
            } else {
                println "Link do arquivo auxiliar não encontrado."
            }
        } catch (Exception e) {
            println "Erro ao baixar arquivo auxiliar: ${e.message}"
        }
    }

    static String obterLinkCompleto(String link) {
        if (link.startsWith("/")) {
            return BASE_URL + link
        }
        return link
    }

    static void baixarArquivo(String url, String nomeArquivo) {
        try {
            URL website = new URL(url)
            def inStream = website.openStream() // Usando 'def' para inferir o tipo
            Files.copy(inStream, Paths.get("$DOWNLOAD_PATH/$nomeArquivo"), StandardCopyOption.REPLACE_EXISTING)
            inStream.close()
            println "$nomeArquivo baixado com sucesso."
        } catch (Exception e) {
            println "Erro ao baixar $nomeArquivo: ${e.message}"
        }
    }

    // Função para salvar dados como .xlsx
    static void salvarComoXlsx(def dados, String caminhoArquivo) {
        try {
            Workbook workbook = new XSSFWorkbook()
            Sheet sheet = workbook.createSheet("Dados")

            // Preenche o conteúdo no arquivo .xlsx
            if (dados instanceof List) {
                for (int i = 0; i < dados.size(); i++) {
                    Row row = sheet.createRow(i)
                    def coluna = dados[i]
                    for (int j = 0; j < coluna.size(); j++) {
                        Cell cell = row.createCell(j)
                        cell.setCellValue(coluna[j])
                    }
                }
            }

            FileOutputStream fileOut = new FileOutputStream(caminhoArquivo)
            workbook.write(fileOut)
            fileOut.close()
            workbook.close()

            println "Arquivo salvo com sucesso em: $caminhoArquivo"
        } catch (Exception e) {
            println "Erro ao salvar arquivo como .xlsx: ${e.message}"
        }
    }
}
