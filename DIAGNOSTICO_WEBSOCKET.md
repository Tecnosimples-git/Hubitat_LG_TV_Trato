# Diagnóstico de Desconexões do Websocket LG TV

> Guia para identificar se as desconexões intermitentes do websocket são causadas pelo driver, pela TV ou pela rede.

## Como usar os atributos de diagnóstico

O driver agora expõe 4 atributos de diagnóstico na aba **Current States** do dispositivo:

| Atributo | Tipo | Significado |
|----------|------|-----------|
| `disconnectCount` | NUMBER | Quantas vezes o websocket caiu desde a última inicialização |
| `lastDisconnectAt` | STRING | Timestamp da última queda (`yyyy-MM-dd HH:mm:ss`) |
| `reconnectCount` | NUMBER | Quantas vezes o driver tentou reconectar |
| `lastReconnectAt` | STRING | Timestamp da última tentativa de reconexão |

### O que observar

- Se `disconnectCount` sobe constantemente a cada poucos minutos → **problema na rede ou na TV**.
- Se `disconnectCount` sobe apenas quando você manda um comando que a TV rejeita → **problema no driver**.
- Se `disconnectCount` = 0 e tudo funciona → **não há problema**.

---

## Árvore de diagnóstico

### 1. As desconexões acontecem em horários específicos?

**Sim, sempre no mesmo horário (ex: 02:00 da manhã)**
→ A TV pode estar fazendo auto-update, reiniciando serviços ou entrando em modo de economia de energia agressiva.

**Não, são aleatórias**
→ Vá para o passo 2.

---

### 2. O Hubitat e a TV estão na mesma sub-rede?

**Não sei / Não tenho certeza**
- Verifique o IP do Hubitat (hub.localIP) e o IP da TV (ipAddr nas preferências).
- Se os 3 primeiros octetos são diferentes (ex: Hub=192.168.0.x, TV=192.168.1.x) → **problema de roteamento / sub-rede**.
  - Solução: coloque ambos na mesma sub-rede ou configure roteamento de multicast.

**Sim, mesma sub-rede**
→ Vá para o passo 3.

---

### 3. A TV perdeu o IP (DHCP)?

**O IP da TV muda?**
- Se o IP da TV mudou, o driver não consegue mais se conectar → **reserve IP via DHCP no roteador**.
- No Hubitat, atualize o IP nas preferências do dispositivo.

**IP está fixo/reservado e correto**
→ Vá para o passo 4.

---

### 4. A TV desliga o websocket por inatividade?

**A TV desconecta após X minutos sem comando**
→ Isso é comportamento normal de algumas TVs LG. O driver já tem `pingInterval` e `fastPing` para lidar com isso.
- Ajuste o **Ping interval** nas preferências (padrão: 5 minutos).
- Se a TV desconecta em menos de 5 minutos, diminua para 1-2 minutos.

**Desconecta mesmo com comandos frequentes**
→ Vá para o passo 5.

---

### 5. Problema de WiFi vs Cabo?

**A TV está no WiFi?**
- WiFi pode ter picos de latência ou quedas rápidas que derrubam websockets.
- Teste: conecte a TV via **cabo Ethernet** e observe se as desconexões param.
- Se pararam → **problema de WiFi**. Considere usar cabo ou um AP mais próximo.

**A TV já está no cabo e mesmo assim cai**
→ Vá para o passo 6.

---

### 6. Conflito com outros apps/controles?

**Outro app (LG ThinQ, Home Assistant, etc.) está conectado à TV ao mesmo tempo?**
- A TV webOS aceita **apenas 1 conexão websocket de controle** por vez (ou poucas).
- Se outro app está conectado, a TV pode derrubar o Hubitat.
- **Solução:** desconecte outros apps de controle remoto da TV ou configure-os para não manter conexão persistente.

**Nenhum outro app está conectado**
→ Vá para o passo 7.

---

### 7. A TV rejeita a conexão por certificado SSL?

**Se você desativar SSL nas preferências, as desconexões param?**
- Vá em Preferences → desmarque **Enable SSL** → Salve.
- Se parou → **problema de SSL/certificate** na TV ou no Hubitat.
- Se não parou → **não é SSL**.

**Importante:** deixe SSL desativado apenas para teste. Em produção, prefira SSL ligado para segurança.

---

### 8. Firmware da TV está desatualizado?

**A TV tem firmware antigo?**
- Vá em Configurações da TV → Suporte → Atualização de Software.
- TVs com firmware muito antigo (>2 anos) podem ter bugs no websocket.
- **Atualize o firmware da TV** se houver atualização disponível.

---

## Checklist rápido de rede

Rode estes comandos no Hubitat (ou em um PC na mesma rede) para testar conectividade:

```groovy
// No Hubitat, vá em Apps > Maker API ou Rule Machine e teste:
// Ping contínuo para a TV
```

Ou, de um PC na mesma rede:

```bash
# Teste de ping contínuo (Windows)
ping -t <IP_DA_TV>

# Teste de porta (Windows, precisa do TelnetClient ativado)
telnet <IP_DA_TV> 3000   # sem SSL
telnet <IP_DA_TV> 3001   # com SSL
```

- Se o ping tem perda de pacotes (packet loss) → **problema de rede**.
- Se a porta 3000/3001 fecha aleatoriamente → **a TV está derrubando a porta**.

---

## Conclusão: quem é o culpado?

| Sintoma | Causa mais provável |
|---------|---------------------|
| Desconexões aleatórias, ping com perda | Rede (WiFi, roteador, cabo) |
| Desconexões sempre após X minutos de inatividade | TV (timeout de websocket) |
| Desconexões quando outro app LG está aberto | Conflito de conexão (limite da TV) |
| Desconexões só com SSL ligado | Certificado/TLS da TV |
| Desconexões após update do firmware | Bug da LG no firmware novo |
| Desconexões só em horários específicos | Auto-update / modo eco da TV |

## Se nada resolver

Se após todos os testes ainda houver desconexões, **o driver original de Dan Danache provavelmente também terá o mesmo comportamento**, já que o core de comunicação é idêntico. Neste caso:

1. Teste com o driver original (sem o fork) para confirmar.
2. Se o original também cai → reporte na thread da comunidade Hubitat do driver original.
3. Se o original NÃO cai → nos avise com os logs comparativos e corrigimos.
