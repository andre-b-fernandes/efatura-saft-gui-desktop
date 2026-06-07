# Requisitos mínimos de utilização

Será necessário ter instalado a versão OpenJDK 8 ou Java 8 SE ou superior no classpath da máquina para que seja possível proceder à execução do comando batch. Poderá verificar qual a versão instalada executando o seguinte comando na linha de comandos "java -version". Obter java aqui.

No envio, o relógio da máquina terá de se encontrar sincronizado com o definido pelo Observatório Astronómico de Lisboa. Caso o relógio não se encontre sincronizado será retornado uma erro na autenticação, indicando que "ERROR CODE: 11: Validade da credencial expirada".

Atenção que o processo será interrompido caso existam inconsistências a nível dos totais dos documentos no ficheiro submetido. Durante um período de tempo de adaptação o utilizador poderá optar por continuar com o processo e submeter o ficheiro, mas brevemente deixarão de ser aceites ficheiros nestas condições.

Para efetuar o envio do ficheiro é necessário ligação à internet, se pretender apenas efetuar a validação poderá não dispor de ligação à internet.

# Parâmetros de invocação da aplicação

Opção	Opção extendida	Descrição	Obrigatório	Exemplo de utilização	Validações
-XMS	-	Define o tamanho inicial e mínimo de memória para iniciar a aplicação. É definido através de um inteiro seguido da unidade de memória que este representa. Os valores possíveis para a unidade de memória são: g|G|m|M|k|K.
Este valor terá de ser indicado antes do parâmetro "-jar".	Não	-XMS64m	-
-XMX	-	Define o tamanho máximo de memória que a aplicação poderá utilizar. É definido através de um inteiro seguido da unidade de memória que este representa. Valores possíveis para a unidade de memória: g|G|m|M|k|K. Este valor terá de ser indicado antes do parâmetro "-jar".	Não	-XMX1024m	-
-jar	-	Define o ficheiro jar onde se encontra a aplicação de envio de ficheiro SAF_T (PT). Caso o caminho para o ficheiro jar contenha espaços este terá de ser delimitado por aspas.	Sim	-jar FACTEMICLI-[VERSAO]-cmdClient.jar	-
-n	--nif	Define o NIF do emitente para o qual se pretende enviar o ficheiro SAFT-T (PT) e que será utilizado para autenticação no portal. Poderá ser definido o sub-utilizador, através da seguinte formatação 123456789/1.	Sim	-i 123456789	-Terá de ser um NIF válido.
-p	--password	Password do utilizador, no Portal das Finanças, para o qual se pretende realizar o envio de ficheiros.	Sim	-n xxxxxxxxx	- Password terá de ser válida para o contribuinte em questão.
-a	--ano	Ano a que se refere o ficheiro a enviar.	Sim	-a 2013	- Terá de ser um valor numérico;
- Dimensão igual a 4.
-m	--mes	Mês a que se refere o ficheiro a enviar.	Sim	-m 01	- Terá de ser um valor numérico;
- Dimensão igual a 2.
-op	--operacao	Indicação da operação que se pretende realizar, com um dos seguintes valores:
- "validar": Valida o ficheiro definido no parâmetro '-i';
- "enviar": Envia o ficheiro, definido no parâmetro '-i', para a AT.	Sim	-op enviar	- O valor definido para a operação terá de ser um dos especificados.
-i	--input	Indicação do caminho para o ficheiro que pretende enviar/validar para a AT. Se o caminho contiver espaços este terá de ser delimitado por aspas.	Sim	-i "c:\Ficheiro.xml"	- Ficheiro tem de existir em disco.
-o	--output	Indicação do caminho para o ficheiro onde será escrito o resultado do envio do ficheiro. Se o caminho contiver espaços este terá de ser delimitado por aspas. Por omissão escreve para a consola onde foi iniciado o envio.	Não	-o "c:\Ficheiro resultado.xml"	-Pasta onde será colocado o ficheiro terá de existir em disco.
-md	--multidoc	Indicação do caminho para o ficheiro onde será escrito o ficheiro multidocumento. Se o caminho contiver espaços este terá de ser delimitado por aspas. Por omissão escreve o ficheiro na mesma directoria e nome, com sufixo 'multidocumento', do ficheiro definido no parâmetro '-i'.	Não	-md "c:\Ficheiro multidocumento.xml"	-
-t	--testes	Indicação de que se trata de um envio de testes, devendo o ficheiro ser ignorado para processamento.	Não	-t	
-c	--caminho	Caminho completo onde se pretende guardar o jar atualizado do cliente.	Não	-c "C:\caminho\onde\guardar\o\jar"	- Se for indicado e for detectado que o jar está desatualizado, o downoload é efetuado automaticamente bem como a submissão do ficheiro com o jar atualizado.
- Sendo o parâmetro não obrigatório, caso este não seja indicado, se for detectado que o jar está desatualizado será pedido o caminho ao utilizador no decorrer do processo.
-af	--autofaturacao	Indicação de que se trata de um envio de autofaturação.	Não	-af	- Sendo o parâmetro não obrigatório, caso este não seja indicado, será efetuada uma submissão normal de ficheiro.
-ea	--emitenteAutofaturacao	Define o NIF do fornecedor dos bens ou prestador dos serviços que consta no ficheiro, obrigatório no caso presença do parâmetro -af.	Não	-ea 123456789	- Terá de ser um NIF válido.
- Este parâmetro é obrigatório quando há indicação de autofaturação (-af).
-h	--help	Imprime a listagem com todo os parâmetros existentes, bem como um exemplo de utilização.	Não	-h	
Versão Saft
A versão indicada no ficheiro a enviar (no elemento "AuditFileVersion") poderá ser um dos seguintes valores:

"1.02_01": Portaria n.º 160/2013;
"1.03_01": Portaria n.º 274/2013;
"1.04_01": Portaria n.º 302/2016.
O formato "1.01_01" (Portaria n.º 1192/2009) deixou de ser aceite a partir de 1 de Abril de 2014.
Exemplo de utilização
A configuração mínima para proceder ao envio do ficheiro é a seguinte:

java -jar FACTEMICLI-[VERSAO]-cmdClient.jar -n 123456789 -p xxxxxxxxx -a 2013 -m 01 -op enviar
    -i "C:\caminho para ficheiro\Nome_ficheiro.xml"
Caso seja pretendido é possível indicar um ficheiro de saída para onde será escrito o resultado do processamento ou alterar a quantidade de memoria utilizada:

java -Xms:256m -Xmx:1024m -jar FACTEMICLI-[VERSAO]-cmdClient.jar -n 123456789/14 -p xxxxxxxxx -a 2013 -m 01 -op enviar
    -i "C:\caminho para ficheiro\Nome_ficheiro.xml" -o "C:\caminho para ficheiro\Nome_ficheiro_saida.xml"
Exemplo de caso em que o ficheiro submetido contém inconsistências nos totais e o utilizador opta por continuar com o processo:

java -jar FACTEMICLI-[VERSAO]-cmdClient.jar -n 123456789 -p xxxxxxxxx -a 2013 -m 01 -op enviar -i "C:\caminho para ficheiro\Nome_ficheiro.xml"

Informa-se que os elementos de controlo do ficheiro apresentam diferenças, pelo que será oportuno contactar a empresa produtora de software.
Brevemente não será possível comunicar ficheiros que evidenciem estas anomalias.

                           |    NumberOfEntries    NumberOfEntries Calculado    TotalDebit    TotalDebit Calculado    TotalCredit    TotalCredit Calculado
-----------------------------------------------------------------------------------------------------------------------------------------------------------
   Documentos de Faturação |                 17                           17        950.00                  950.00       19846.12              18846.12000
 Documentos de Conferência |                  4                            4       0.00000                       0       10500.00              10500.00000
                   Recibos |                  4                            4          0.00                       0       1428.616                 1428.616

Deseja continuar a comunicação dos documentos mesmo com esta anomalia? (s/n)

s
Estrutura de resposta (XML)
Nesta secção descreve-se a estrutura e informação da resposta ao envio de ficheiro através do aplicativo batch. A especificação XSD da resposta enviada pelo servidor é a seguinte:

```xml
<?xml version="1.0"?>
        <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema">
        <xs:element name="response">
            <xs:complexType>
                <xs:choice>
                      <xs:element name="errors" type="errorType" minOccurs="1"/>
                      <xs:sequence>
                            <xs:element name="totalFaturas" type="xs:string" maxOccurs="1" minOccurs="1"></xs:element>
                            <xs:element name="totalCreditos" type="xs:string" maxOccurs="1" minOccurs="1"></xs:element>
                            <xs:element name="totalDebitos" type="xs:string" maxOccurs="1" minOccurs="1"></xs:element>
                            <xs:element name="warning" type="xs:string" maxOccurs="1" minOccurs="0"></xs:element>
                            <xs:element name="idFicheiro" type="xs:string" maxOccurs="1" minOccurs="0"></xs:element>
                            <xs:element name="nomeFicheiro" type="xs:string" maxOccurs="1" minOccurs="1"></xs:element>
                            <xs:element name="createdDate" type="xs:string" maxOccurs="1" minOccurs="1"></xs:element>
                      </xs:sequence>
                </xs:choice>
                <xs:attribute name="code" type="xs:string" use="required"/>
            </xs:complexType>
        </xs:element>
        <xs:complexType name="errorType">
            <xs:sequence>
                <xs:element name="error" type="xs:string" maxOccurs="unbounded" minOccurs="1"/>
            </xs:sequence>
        </xs:complexType>
    </xs:schema>
```
Exemplo de resposta de sucesso no envio do ficheiro. No caso de o ficheiro ser aceite mas com alguma condicionante, será colocada uma mensagem de "warning".

```xml
<?xml version="1.0" encoding="ISO-8859-1"?>
    <response code="200">
        <totalFaturas>10</totalFaturas>
        <totalCreditos>1234.56</totalCreditos>
        <totalDebitos>12.34</totalDebitos>
        <warning>Devido a todas as faturas serem anteriores a 1/Jan/2013 o ficheiro não será considerado para processamento.</warning>
        <idFicheiro>123</idFicheiro>
        <nomeFicheiro>saft-pt.xml</nomeFicheiro>
        <createdDate>2013-02-01 15:17:54</createdDate>
    </response>
```
Exemplo de resposta quando existe um erro no envio ou validação do ficheiro.

```xml
<?xml version="1.0" encoding="ISO-8859-1"?>
    <response code="-3">
        <errors>
            <error>NIF do emitente ('123456789') é diferente do NIF declarado no ficheiro ('987654321').</error>
        </errors>
    </response>
```
Códigos de resposta existentes
Código de Resposta	Mensagem de erro	Descrição do erro
-1	Ocorreu um erro durante o envio do ficheiro.	Erro genérico na comunicação entre cliente e servidor.
-2	O ficheiro recebido não tem o mesmo tamanho que o ficheiro enviado.	Tamanho do ficheiro declarado no header pelo programa cliente não corresponde ao tamanho real enviado
-3	Mensagem específica da validação que não está a ser respeitada.	A mensagem de resposta para este erro é variável, estando a sua mensagem de erro dependente da validação que não é respeitada
-4	Ocorreu um erro durante o envio do ficheiro.	Erro ao inserir o ficheiro na base de dados
-5	O ficheiro selecionado já foi enviado para a AT.	Cenário em um ficheiro idêntico foi previamente enviado para a AT.
-6	Erro no processo de conversão.	Este erro ocorre caso exista algum problema durante o processo de conversão. É apresentada mensagem complementar indicando a origem do erro.
-7	O cliente de linha de comandos que está a utilizar não se encontra atualizado. Por favor aceda ao portal e-fatura e obtenha a nova versão.	Caso o cliente linha de comando que se encontra a utilizar não seja a versão mais actual.
-8	O ficheiro resumido não pode ser o mesmo que o ficheiro seleccionado para envio.	Caso em que o ficheiro indicado no parâmetro -i (ficheiro a enviar para a AT) é o mesmo que o indicado pelo parâmetro -r (localização do ficheiro resumido).
-9	Para poder entregar o ficheiro na versão que indicou necessita de atualizar o cliente de linha de comandos. Para isso, por favor, aceda ao portal e-fatura e obtenha a nova versão.	Caso o cliente linha de comando que se encontra a utilizar não seja a versão mais actual para o formato de ficheiro que está a entregar.
-401	Login failed for user 123456789. ERROR CODE: <ERRO ANTENTICAÇÃO>	Quando ocorre um erro na autenticação do servidor
-666	Ocorreu um erro.	Erro não categorizado durante o processo de envio. É apresentada uma mensagem descritiva do erro.
200	-	Sucesso no envio do ficheiro