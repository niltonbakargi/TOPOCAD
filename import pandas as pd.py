import pandas as pd
import os

def perguntar_cota_baixa():
    """
    Pergunta ao usuário se deseja usar o ponto com a cota mais baixa.
    """
    while True:
        resposta = input("Quer usar o ponto com a cota mais baixa? (s/n): ").lower()
        if resposta == 's':
            return True
        elif resposta == 'n':
            return False
        else:
            print("Resposta inválida. Por favor, responda com 's' para sim ou 'n' para não.")

def perguntar_altura_cota():
    """
    Pergunta ao usuário a altura da cota mais baixa desejada.
    """
    while True:
        try:
            altura_cota = float(input("Digite a altura da cota mais baixa desejada: "))
            return altura_cota
        except ValueError:
            print("Altura inválida. Por favor, digite um número válido.")

def processar_arquivo_csv(file_path):
    """
    Processa o arquivo CSV detectando o delimitador e converte as colunas para numérico.
    """
    delimiters = [',', ';', '\t']
    df = None

    for delimiter in delimiters:
        try:
            df = pd.read_csv(file_path, delimiter=delimiter, header=None, skiprows=1)
            if df.shape[1] == 5:
                print(f"\nDelimitador detectado para o arquivo '{file_path}': '{delimiter}'")
                break
        except pd.errors.ParserError:
            continue
        except Exception as e:
            print(f"Erro ao ler o arquivo CSV {file_path}: {e}")
            return None

    if df is None or df.shape[1] != 5:
        print(f"Erro ao ler o arquivo CSV {file_path} com os delimitadores {delimiters}. Verifique o formato do arquivo.")
        return None

    df.columns = ['ID', 'X', 'Y', 'Cota de Altura', 'Descrição']

    for col in ['X', 'Y', 'Cota de Altura']:
        try:
            df[col] = pd.to_numeric(df[col], errors='coerce')
        except ValueError:
            print(f"Erro ao converter a coluna {col} do arquivo {file_path} para numérico. Verifique os dados.")

    if df.empty:
        print(f"Nenhum dado fornecido no arquivo {file_path}.")
        return None

    return df.sort_values(by='Cota de Altura')

def criar_script_autocad(df_sorted, arquivo_csv):
    """
    Cria um script para AutoCAD a partir dos dados processados e salva na pasta 'planilha_do_excel'.
    """
    script_autocad = ""
    pontos_por_descricao = {}
    layer_set_numero = 1

    for index, row in df_sorted.iterrows():
        descricao = str(row['Descrição']) if isinstance(row['Descrição'], float) else row['Descrição']
        descricao = descricao.replace(' ', '_')
        ponto = f"{row['Y']:.3f},{row['X']:.3f},{row['Cota de Altura']:.3f}"  # Inverte as coordenadas X e Y

        if descricao in pontos_por_descricao:
            pontos_por_descricao[descricao].append((ponto, row['Cota de Altura']))
        else:
            pontos_por_descricao[descricao] = [(ponto, row['Cota de Altura'])]

    for descricao, pontos_cota in pontos_por_descricao.items():
        script_autocad += f"layer set {layer_set_numero:02d} \n"  # Adiciona um espaço após o número do layer
        script_autocad += "insert pontos\n"

        first_line = True
        for ponto, cota in pontos_cota:
            if first_line:
                script_autocad += f"{ponto}    {cota:.3f} - {descricao}\n"
                first_line = False
            else:
                script_autocad += f"  {ponto}    {cota:.3f} - {descricao}\n"  # Adiciona dois espaços no início da linha
        layer_set_numero += 1

    nome_arquivo = os.path.join('planilha_do_excel', arquivo_csv.split('.')[0] + "_script.scr")
    with open(nome_arquivo, 'w') as file:
        file.write(script_autocad)

    print(f"Script para AutoCAD criado: {nome_arquivo}")
    return script_autocad

def main():
    """
    Função principal que gerencia o fluxo do programa.
    """
    if not os.path.exists('planilha_do_excel'):
        os.makedirs('planilha_do_excel')

    arquivos_csv = [arquivo for arquivo in os.listdir('planilha_do_excel') if arquivo.endswith('.csv')]

    if not arquivos_csv:
        print("Nenhum arquivo CSV encontrado na pasta 'planilha_do_excel'.")
        return

    for arquivo_csv in arquivos_csv:
        file_path = os.path.join('planilha_do_excel', arquivo_csv)
        df_sorted = processar_arquivo_csv(file_path)

        if df_sorted is None:
            continue

        print(f"\nTabela Ordenada por Altura - {arquivo_csv}:")
        print(df_sorted)

        usar_cota_baixa = perguntar_cota_baixa()

        while not usar_cota_baixa and not df_sorted.empty:
            df_sorted = df_sorted[df_sorted['Cota de Altura'] != df_sorted['Cota de Altura'].iloc[0]]
            if df_sorted.empty:
                print("Não há mais pontos para escolher.")
                break
            print("\nTabela atualizada:")
            print(df_sorted)
            usar_cota_baixa = perguntar_cota_baixa()

        if not df_sorted.empty:
            if usar_cota_baixa:
                altura_cota = perguntar_altura_cota()
                df_sorted['Cota de Altura'] -= df_sorted['Cota de Altura'].iloc[0]
                df_sorted['Cota de Altura'] += altura_cota

            df_sorted = df_sorted.sort_values(by='Descrição')
            print(f"\nTabela Ordenada por Descrição - {arquivo_csv}:")
            print(df_sorted)
        else:
            print(f"Todos os pontos foram removidos do arquivo {arquivo_csv}.")
            continue

        criar_script = input("Deseja criar um script para AutoCAD? (s/n): ").lower() == 's'

        if criar_script:
            script_autocad = criar_script_autocad(df_sorted, arquivo_csv)
            print("Script para AutoCAD:")
            print(script_autocad)
        else:
            print("Script para AutoCAD não foi criado.")

if __name__ == "__main__":
    main()
    