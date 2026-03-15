# Topocad
Aplicativo para Tabulação de Dados Topográficos de Estação Total para AutoCAD

## O que faz

1. Busca arquivos `.csv` na pasta **Downloads** do dispositivo e exibe a lista para seleção
2. Processa o CSV detectando automaticamente o delimitador (`,` `;` ou `tab`)
3. Exibe os pontos carregados em tela
4. Guia o usuário no ajuste de cota de referência (remove o ponto mais baixo até o usuário aceitar um como base)
5. Aplica o ajuste de elevação e gera um script `.scr` para AutoCAD salvo em **Downloads**

## Formato do CSV

Cinco colunas na ordem: `ID, X, Y, Cota, Descrição`

## Script gerado

- **Layer 01** — todos os pontos com nome original completo
- **Layers 02+** — pontos agrupados pelas 3 primeiras letras da descrição
- Layers criados automaticamente se não existirem
- Separador decimal: ponto (`.`)
- Coordenadas na ordem `Y, X, Z` (padrão AutoCAD)

## Requisitos

- Android 5.0+ (API 21)
- Permissão de acesso a arquivos (solicitada no primeiro uso)

## Build

Abrir no Android Studio e executar `Run`.
