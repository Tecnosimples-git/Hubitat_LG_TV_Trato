# Hubitat LG TV Trato

## Visão Geral
Fork do driver "LGTV with webOS" de Dan Danache, adaptado para compatibilidade com a plataforma VETRA / GW8 do ecossistema Hubitat.

## Stack
- Groovy (Hubitat Drivers)

## Arquivos
- `Hubitat_LG_TV_Trato_v1.8.3-TRATO-1.0.groovy` — Driver principal (Parent)
- `Hubitat_LG_TV_Trato_Remote_v1.8.3-TRATO-1.0.groovy` — Child device (Remote simulation)

## Regras Locais
- Este é um fork; manter referência ao autor original (Dan Danache) e URL original
- **O core de comunicação webOS NUNCA deve ser alterado.** Todas as adições VETRA devem ser feitas apenas como aliases no final do arquivo.
- O driver pai e o child devem sempre ser mantidos em sincronia de versão
- Novos comandos/attributes devem seguir o padrão de nomenclatura GW8 já estabelecido nos drivers TRATO MolSmart

## Decisões Arquiteturais
- **Core original preservado:** websocket, parsing, Wake-on-LAN, subscriptions, etc. são idênticos ao driver original.
- **SamsungTV capability** adicionada para que a VETRA reconheça o dispositivo como TV.
- **PushableButton capability** com mapeamento de 58 botões idêntico ao GW8 para compatibilidade total de dashboards.
- **Variable capability** placeholder para leitura de atributos extras pela VETRA.
- Comandos de navegação (`arrowUp`, `arrowDown`, `enter`, `back`, etc.) traduzem para `pushRemoteButtons('UP')`, etc., que utiliza o child device de simulação remota — exatamente como o driver original já fazia.
- Comandos de apps/inputs (`hdmi1`, `appNetflix`, `appYouTube`, `appHBO`, `appDisney`) traduzem para `startActivity()` com os appIds da webOS.
- Mapeamento de pictureMode/soundMode SamsungTV → LG feito via helpers simples no final do arquivo.
- Comando `openAppById(String appId)` genérico adicionado para compatibilidade com apps que têm IDs variáveis por região (HBO Max, Disney+, etc.).
- Comando `listApps()` adicionado para diagnóstico — lista todos os apps instalados na TV para descobrir IDs exatos.

## Requisito de funcionamento
Para que os comandos de navegação (setas, enter, back, números, botões coloridos) funcionem, a preferência **"Simulate TV Remote"** deve estar **habilitada**. Sem isso, `pushRemoteButtons` apenas loga um aviso e retorna — comportamento idêntico ao driver original.

## Referências
- Driver original: https://codeberg.org/dan-danache/hubitat/src/branch/main/lgtv-drivers
- HPM do original: https://community.hubitat.com/t/release-lgtv-with-webos/148892
- Padrão GW8/TRATO: ver `Exemplos IR/user_driver_TRATO_MolSmart___GW8___TV__irweb__1200.groovy`
