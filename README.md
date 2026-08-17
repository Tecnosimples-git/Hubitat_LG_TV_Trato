# LG TV Fork TRATO

Driver para integrar TVs LG com webOS ao Hubitat Elevation e à plataforma
VETRA/GW8.

## Compatibilidade

- Hubitat Elevation
- TVs LG com webOS acessíveis pela rede local
- Plataforma VETRA/GW8

## Antes de instalar

- Conecte o Hubitat e a TV à mesma rede.
- Reserve um endereço IP fixo para a TV no roteador.
- Deixe a TV ligada durante a primeira configuração.

## Instalação

1. No Hubitat, abra **Drivers Code** e selecione **New Driver**.
2. Cole o conteúdo de
   `Hubitat_LG_TV_Trato_v1.8.3-TRATO-1.0.groovy` e salve.
3. Crie outro driver com o conteúdo de
   `Hubitat_LG_TV_Trato_Remote_v1.8.3-TRATO-1.0.groovy` e salve.
4. Acesse **Devices**, selecione **Add Device** e depois **Virtual**.
5. Escolha o driver **LG TV Fork TRATO**.
6. Informe o endereço IP da TV e salve as preferências.
7. Quando a TV solicitar autorização, confirme o pareamento.

## Configuração recomendada

- Mantenha **SSL** habilitado.
- Habilite **Simulate TV Remote** para usar setas, números, Enter, Voltar e
  botões coloridos.
- Depois de alterar o IP ou outra preferência, clique em **Save Preferences**.

Comandos de volume, mute, canais, entradas HDMI e abertura de aplicativos não
dependem da opção **Simulate TV Remote**.

## Uso com a VETRA

Depois da instalação e do pareamento, a TV pode ser adicionada à VETRA para
controle de volume, canais, navegação, entradas e aplicativos.

O driver inclui comandos para Netflix, YouTube, Amazon Prime Video, HBO Max e
Disney+. A disponibilidade de cada aplicativo depende do modelo da TV, da
região e dos aplicativos instalados.

## Créditos

Este projeto é baseado no driver
[LGTV with webOS](https://codeberg.org/dan-danache/hubitat/src/branch/main/lgtv-drivers),
de Dan Danache, versão 1.8.3.

Adaptação para VETRA/GW8 desenvolvida por **ryuleal / TecnoSimples**. Consulte
o projeto original para as condições das licenças MIT e Unlicense.
