# Echoes (JoJo Stand) — NeoForge 1.21.1

Mod com o Stand **Echoes** (ACT 1, ACT 2 e ACT 3), com modelos baseados nas suas imagens.

## Como compilar
1. Instale o **JDK 21** e o **IntelliJ IDEA** (Community serve).
2. Abra a pasta do projeto no IntelliJ (`File > Open` e escolha a pasta, ele reconhece o Gradle sozinho).
   - Se não houver `gradlew`, rode uma vez `gradle wrapper` (precisa de Gradle instalado) ou deixe o IntelliJ sincronizar.
3. Para testar: execute a tarefa Gradle `runClient`.
4. Para gerar o `.jar`: `./gradlew build` (sai em `build/libs/echoes-0.1.0.jar`, coloque em `mods/`).

## Como jogar
- Pegue a **Flecha de Stand** (`/give @s echoes:stand_arrow`, aba Ferramentas no criativo) e clique com o botão direito para despertar.
  Ou use `/echoes awaken` (OP). Para testar todos os Atos: `/echoes unlock 3`.
- Teclas (mudam em Controles > "Echoes (JoJo)"):

| Tecla | Ação |
|---|---|
| R | Invocar / recolher o Echoes |
| X | Trocar de ACT (depois de desbloqueado) |
| Z C V B N M | Ataques 1 a 6 (o 6 é a Suprema) |
| G | Trocar a palavra da SONIC CATASTROPHE (BOOM / SIZZLE / WHOOSH) |

## Evolução
Cada ataque usado dá pontos de domínio (a Suprema dá mais). 100/100 no ACT 1 libera o ACT 2; 100/100 no ACT 2 libera o ACT 3.
(A missão especial e o chefe do documento ainda não existem, ficaram para a próxima etapa.)

## Estrutura
- `client/EchoesModel` — os 3 modelos (cubos em código) + animação. Cores vêm de faixas do PNG (veja o comentário na classe).
- `logic/EchoesLogic` — os 18 ataques, cooldowns, troca de ATO e domínio.
- `logic/Traps`, `Status`, `Scheduler` — armadilhas (SOUND TRAP/FREEZE!), estados Pesado/Congelado e tarefas com atraso.
- `assets/echoes/textures/entity/echoes_act1..3.png` — texturas (pode repintar à vontade, mantendo as 5 faixas de cor).

## Ainda não feito (do seu documento)
Word Combo do ACT 2, missão/chefe para evoluir, letras de som como partículas próprias, animações de barragem mais elaboradas, sons próprios.
