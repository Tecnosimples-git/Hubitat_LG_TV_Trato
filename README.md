# LG TV Fork TRATO

> Fork do driver **LGTV with webOS** de Dan Danache, adaptado para plena compatibilidade com a plataforma **VETRA / GW8**.

## Origem
- **Driver original:** [LGTV with webOS](https://codeberg.org/dan-danache/hubitat/src/branch/main/lgtv-drivers) por Dan Danache (v1.8.3)
- **Licença original:** MIT / Unlicense (ver repositório original)

## Filosofia deste fork

**O core de comunicação webOS é idêntico ao original.** Nenhuma linha de websocket, Wake-on-LAN, parsing de mensagens ou gerenciamento de conexão foi alterada.

A única alteração é uma **camada fina de aliases** no final do arquivo:
- Comandos com nomes GW8 / SamsungTV (`arrowUp`, `enter`, `back`, `num1`, `btnA`, `appNetflix`, etc.)
- Capabilities extras (`SamsungTV`, `PushableButton`, `Variable`) para o reconhecimento pela VETRA
- Atributos extras (`power`, `channel`, `volume`, `picture`, `sound`, `movieMode`)
- Mapeamento `push(N)` de 58 botões padrão GW8

## Requisito importante

Para que os comandos de **navegação** (setas, enter, back, números, botões coloridos) funcionem, a opção **"Simulate TV Remote"** nas preferências do driver deve estar **habilitada**. Isso cria o child device `LGTV Remote TRATO`, que abre um websocket secundário de input remoto da TV — exatamente como o driver original já fazia.

Comandos como **volume, mute, canais, HDMI, apps** (YouTube, Netflix, etc.) **não dependem** do `simulateRemote` e funcionam via API webOS nativa.

## Instalação no Hubitat

1. Vá em **Drivers Code** > **New Driver**.
2. Cole o conteúdo de `LG_TV_Fork_TRATO.groovy`.
3. Salve.
4. Cole o conteúdo de `LGTV_Remote_TRATO.groovy`.
5. Salve.
6. Vá em **Devices** > **Add device** > **Virtual**.
7. Selecione o driver **LG TV Fork TRATO**.
8. Configure IP da TV, SSL (recomendado) e habilite **"Simulate TV Remote"**.
9. Salve e aprove o pareamento na tela da TV.

## Compatibilidade VETRA

O driver expõe:
- `SamsungTV` → reconhecido pela VETRA como TV
- `PushableButton` → permite controle via `push(N)` com mapeamento GW8 de 58 botões
- `Variable` → atributos extras para dashboards
- Comandos nomeados no padrão GW8 para automações e scripts

## Apps suportados (YouTube, Netflix, Amazon, HBO, Disney+)

O driver possui comandos prontos para abrir os principais apps:
- `appNetflix()` → Netflix
- `appYouTube()` → YouTube
- `appAmazonPrime()` → Amazon Prime Video
- `appHBO()` → HBO Max (tenta IDs comuns; ver abaixo)
- `appDisney()` → Disney+ (tenta IDs comuns; ver abaixo)

### Se HBO Max ou Disney+ não abrirem

Os IDs de app variam por **região** e **modelo da TV**. Se os comandos não funcionarem, você pode descobrir o ID exato da sua TV:

1. Na aba **Commands**, clique em **`listApps`**. O driver envia um comando para a TV listar todos os apps instalados. Verifique os **Logs** do Hubitat para ver a resposta completa com todos os `appId`.
2. Alternativamente, use o comando genérico:
   - `openAppById('com.hbo.hbomax')` — substitua pelo ID correto.

**Como descobrir o ID na sua TV (método Homebridge/Home Assistant):**
- Abra o app desejado na TV manualmente.
- Verifique os logs do Hubitat. O driver loga o `appId` do app em primeiro plano quando muda.
- Ou use o comando `listApps` para listar todos os apps de uma vez.

**IDs comuns por região:**
| App | ID mais comum (Brasil) | ID alternativo |
|-----|------------------------|----------------|
| HBO Max | `com.hbo.hbomax` | `com.hbo.hbomax-prod`, `hbomax`, `hbo.max` |
| Disney+ | `com.disney.disneyplus-prod` | `com.disney.disneyplus`, `disneyplus`, `disney+` |

## Diagnóstico do child device (Simulate TV Remote)

Se o child device `LGTV Remote TRATO` não for criado automaticamente:

1. Verifique nos logs do Hubitat (aba **Logs**) por mensagens começando com `🛠️`.
2. Na aba **Commands** do dispositivo, clique em **`diagnoseChild`** para ver o estado completo:
   - Parent DNI (deve ser o IP convertido em hexadecimal)
   - Child DNI esperado
   - `childDeviceStatus` (mostra `created`, `exists`, `error`, `parent_dni_empty`, etc.)
   - Lista de todos os child devices existentes
3. Se o status for `parent_dni_empty`, preencha o **IP address** nas preferências e salve novamente.
4. Se o status for `error`, certifique-se de que o driver **LGTV Remote TRATO** está instalado em **Drivers Code**.
5. Use **`recreateRemoteChild`** para forçar a recriação do child device.

### State variables que devem aparecer

Após instalar o driver, em **State Variables** você deve ver:
```
activities: [:]
childDeviceStatus: created
lastDisconnectAt: 
lastReconnectAt: 
disconnectCount: 0
reconnectCount: 0
```

Se `childDeviceStatus` não aparecer, o driver ainda não rodou `installed()` ou `updated()`.

## Diagnóstico de desconexões do websocket

Se o websocket da TV desconectar e reconectar de tempos em tempos, consulte:
📄 **[DIAGNOSTICO_WEBSOCKET.md](DIAGNOSTICO_WEBSOCKET.md)**

O driver agora inclui atributos de diagnóstico (`disconnectCount`, `lastDisconnectAt`, `reconnectCount`, `lastReconnectAt`) para ajudar a identificar se o problema está no driver, na TV ou na rede.

## Estrutura

```
LG_TV_Fork_TRATO/
├── AGENTS.md                     # Regras do projeto
├── DIAGNOSTICO_WEBSOCKET.md      # Guia de diagnóstico de desconexões
├── LG_TV_Fork_TRATO.groovy       # Driver principal (core original + camada VETRA)
├── LGTV_Remote_TRATO.groovy      # Componente child (remote simulation)
└── README.md                     # Este arquivo
```

## Changelog

### v1.8.3-TRATO-1.0 (2026-05-22)
- Fork inicial com capabilities VETRA/GW8
- Camada de aliases GW8/SamsungTV adicionada ao final do driver (sem alterar o core original)
- Mapeamento `push(N)` de 58 botões alinhado ao padrão GW8
- Child device renomeado para `LGTV Remote TRATO` no namespace `TRATO`
