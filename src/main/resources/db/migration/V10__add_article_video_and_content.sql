-- ============================================================
-- V10 — Vídeo e conteúdo das aulas
-- ============================================================
-- O seed (V3) criou os 8 artigos sem conteúdo: content ficou NULL
-- em todos, e não havia onde guardar vídeo. Aqui entram o vídeo do
-- YouTube de cada aula e um texto curto que liga o conceito ao jogo.
--
-- Guarda-se só o ID do vídeo, não a URL: o frontend monta o embed
-- (youtube-nocookie) sem precisar interpretar URLs em formatos
-- variados. O CHECK garante o formato de ID do YouTube, então nada
-- além de um ID válido chega ao src do iframe.
--
-- Todos os vídeos foram conferidos antes de entrar aqui: existem
-- (oEmbed 200) e permitem embed (playableInEmbed = true).
-- ============================================================

ALTER TABLE articles
    ADD COLUMN IF NOT EXISTS video_id VARCHAR(11)
        CHECK (video_id IS NULL OR video_id ~ '^[A-Za-z0-9_-]{11}$');

-- Módulo 1 — Matemática Financeira

UPDATE articles SET video_id = 'Y9ng5fVji-A', content =
'Rentabilidade é quanto um investimento rendeu em relação ao valor aplicado, em porcentagem. Se você investe R$ 1.000 e termina com R$ 1.100, a rentabilidade foi de 10%. No RetroBolsa, é a rentabilidade da sua carteira no fim da rodada que define sua posição no ranking — e ela, arredondada, é somada à sua pontuação.'
WHERE id = 'bbbbbbbb-0001-0000-0000-000000000001';

UPDATE articles SET video_id = 'G6LJcuKY80c', content =
'Nos juros simples, o rendimento é sempre calculado sobre o valor inicial. Nos compostos, cada período rende sobre o que já foi acumulado: os juros passam a render juros. R$ 10.000 a 10% ao ano viram R$ 13.000 em 3 anos com juros simples, mas R$ 13.310 com juros compostos. As rodadas do RetroBolsa funcionam assim: o retorno de cada ano se acumula sobre o do ano anterior.'
WHERE id = 'bbbbbbbb-0002-0000-0000-000000000002';

UPDATE articles SET video_id = 'sBCTmBtnJIU', content =
'O retorno anualizado converte um ganho de vários anos em uma taxa equivalente por ano, para comparar investimentos de prazos diferentes. A conta é (valor final ÷ valor inicial) elevado a (1 ÷ anos), menos 1. Uma carteira que foi de R$ 100.000 para R$ 121.000 em 2 anos rendeu 21% no total, mas 10% ao ano. É essa a conta por trás do retorno anual que aparece no resultado da sua rodada.'
WHERE id = 'bbbbbbbb-0003-0000-0000-000000000003';

-- Módulo 2 — Fundamentos de Investimentos

UPDATE articles SET video_id = 'bb0-_GbSg5s', content =
'O P/L (preço sobre lucro) divide o preço da ação pelo lucro por ação da empresa. Ele indica em quantos anos, mantido o lucro atual, a empresa geraria o valor que você pagou pela ação: um P/L de 8 equivale a 8 anos. P/L baixo pode indicar ação barata — ou uma empresa com problemas —, e P/L alto costuma refletir expectativa de crescimento. No RetroBolsa, o P/L aparece no card de cada empresa anônima e ajuda a comparar quem está cara ou barata.'
WHERE id = 'bbbbbbbb-0004-0000-0000-000000000004';

UPDATE articles SET video_id = 'GdtDjDiX2I4', content =
'O ROE (retorno sobre o patrimônio líquido) mede quanto lucro a empresa gera com o capital dos próprios sócios: lucro líquido ÷ patrimônio líquido. Um ROE de 20% significa que cada R$ 100 de patrimônio produziu R$ 20 de lucro no ano. ROE alto e consistente costuma indicar uma empresa que usa bem o capital, mas vale conferir se ele não vem inflado por dívida elevada.'
WHERE id = 'bbbbbbbb-0005-0000-0000-000000000005';

UPDATE articles SET video_id = 'wzt8oS0xIm0', content =
'O Dividend Yield (DY) compara os dividendos pagos por uma ação nos últimos 12 meses com o preço dela: dividendos por ação ÷ preço. Uma ação de R$ 50 que pagou R$ 3 em dividendos tem DY de 6%. DY alto atrai quem busca renda, mas atenção: ele também sobe quando o preço da ação despenca — e aí pode ser sinal de problema, não de oportunidade.'
WHERE id = 'bbbbbbbb-0006-0000-0000-000000000006';

-- Módulo 3 — Macroeconomia

UPDATE articles SET video_id = 'WBNkhIaY7gc', content =
'A Selic é a taxa básica de juros do Brasil, definida a cada reunião do Copom, o comitê de política monetária do Banco Central. Ela é a referência para empréstimos e para a renda fixa, e é a principal ferramenta contra a inflação: juros altos encarecem o crédito, esfriam o consumo e seguram os preços. No RetroBolsa, o título atrelado à Selic rende mais justamente nos cenários de juros altos.'
WHERE id = 'bbbbbbbb-0007-0000-0000-000000000007';

UPDATE articles SET video_id = 'LLANnZaSdQ0', content =
'O IPCA, medido pelo IBGE, é o índice oficial de inflação do Brasil: acompanha a variação de preços de uma cesta de produtos e serviços consumidos pelas famílias. O que importa num investimento é o ganho real, acima da inflação: se a carteira rendeu 8% num ano em que o IPCA foi de 6%, seu poder de compra cresceu só cerca de 1,9%. É por isso que existem títulos IPCA+, como um dos disponíveis no RetroBolsa: pagam a inflação do período mais uma taxa fixa.'
WHERE id = 'bbbbbbbb-0008-0000-0000-000000000008';
