-- ============================================================
-- V15: o que aconteceu de verdade em cada rodada
-- ============================================================
-- competitions.debrief  texto mostrado no resultado, depois da revelação:
--                       o que aconteceu no período e o que dá para aprender.
-- assets.reveal_note    uma frase sobre o ativo, mostrada junto do nome real.
--
-- Os textos descrevem os anos que o motor simula (start_year até
-- end_year - 1) e foram conferidos contra os retornos do seed.
-- As rodadas são localizadas pelo título e pelo ano inicial: em produção
-- a rodada de 2011 foi criada à mão e tem outro id (ver V13).
-- ============================================================

ALTER TABLE competitions ADD COLUMN IF NOT EXISTS debrief TEXT;
ALTER TABLE assets ADD COLUMN IF NOT EXISTS reveal_note TEXT;

-- A descrição da rodada 1 dizia que o país "acabara de obter o grau de
-- investimento" em 2004; isso só aconteceu em 2008.
UPDATE competitions
SET scenario_description = replace(scenario_description,
        'e o país acabara de obter o grau de investimento',
        'e o país buscava o grau de investimento das agências de risco')
WHERE start_year = 2004
  AND scenario_description LIKE '%acabara de obter o grau de investimento%';

-- ------------------------------------------------------------
-- O Grande Boom das Commodities (2004–2010)
-- ------------------------------------------------------------
UPDATE competitions SET debrief =
'De 2004 a 2007, a China comprou minério de ferro, aço e petróleo como nunca, e as empresas brasileiras desses setores dispararam. Com a Selic caindo e o crédito crescendo, os bancos também lucraram muito. Em 2008 veio a crise financeira mundial: a quebra do banco americano Lehman Brothers derrubou as bolsas, e o Ibovespa perdeu mais de 40% no ano. No mesmo ano, o Brasil ganhou o grau de investimento. Quem manteve as ações viu uma recuperação forte em 2009, puxada de novo pela China. O que dá para aprender: empresas lucrativas se recuperaram, mas no caminho chegaram a perder metade do valor em um ano. Ter uma parte em renda fixa ajuda a atravessar uma crise sem precisar vender na baixa.'
WHERE scenario_title = 'O Grande Boom das Commodities (2004–2011)' AND start_year = 2004;

UPDATE assets SET reveal_note = 'Maior produtora de minério de ferro do mundo, vendia boa parte da produção para a China, que crescia perto de 10% ao ano.' WHERE id = 'cccccccc-0001-0000-0000-000000000001';
UPDATE assets SET reveal_note = 'A estatal de petróleo aproveitou a alta do barril e, em 2007, anunciou as descobertas do pré-sal.' WHERE id = 'cccccccc-0002-0000-0000-000000000002';
UPDATE assets SET reveal_note = 'Maior banco privado do país: lucrou com a expansão do crédito e se juntou ao Unibanco em 2008.' WHERE id = 'cccccccc-0003-0000-0000-000000000003';
UPDATE assets SET reveal_note = 'Um dos maiores bancos do país, também cresceu com o crédito e a nova classe média.' WHERE id = 'cccccccc-0004-0000-0000-000000000004';
UPDATE assets SET reveal_note = 'Siderúrgica que vende aço para a construção e a indústria: sofre muito quando a economia mundial freia, como em 2008.' WHERE id = 'cccccccc-0005-0000-0000-000000000005';
UPDATE assets SET reveal_note = 'Título com a taxa travada na compra: com juros altos o período todo, rendeu bem e sem sustos.' WHERE id = 'cccccccc-0006-0000-0000-000000000006';
UPDATE assets SET reveal_note = 'Pagou a inflação mais uma taxa fixa, protegendo o poder de compra em qualquer cenário.' WHERE id = 'cccccccc-0007-0000-0000-000000000007';
UPDATE assets SET reveal_note = 'Rendeu a Selic dia a dia: passou pela crise de 2008 sem oscilar.' WHERE id = 'cccccccc-0008-0000-0000-000000000008';

-- ------------------------------------------------------------
-- Nova matriz econômica (2011–2013)
-- ------------------------------------------------------------
UPDATE competitions SET debrief =
'O plano de crescer com juros baixos e preços controlados não funcionou como esperado: a inflação ficou perto do teto da meta, e em 2013 o Banco Central voltou a subir a Selic. Vendendo gasolina abaixo do preço internacional, a Petrobras perdia dinheiro a cada litro importado, e suas ações caíram. A OGX, que prometia bilhões de barris, encontrou muito menos petróleo que o anunciado: as ações perderam quase todo o valor e a empresa pediu recuperação judicial em 2013. O Ibovespa terminou 2013 abaixo de onde estava no fim de 2010. O que dá para aprender: promessa não é lucro. Olhar se a empresa já dá resultado (lucro positivo, ROE) protege de histórias bonitas.'
WHERE scenario_title = 'Nova matriz econômica (2011–2013)' AND start_year = 2011;

UPDATE assets SET reveal_note = 'Segurou o preço da gasolina abaixo do mercado internacional para conter a inflação, e o prejuízo na importação pesou no lucro.' WHERE id = 'cccccccc-0401-0000-0000-000000000401';
UPDATE assets SET reveal_note = 'Petroleira de Eike Batista: prometeu bilhões de barris, produziu muito menos e pediu recuperação judicial em 2013.' WHERE id = 'cccccccc-0402-0000-0000-000000000402';
UPDATE assets SET reveal_note = 'Com a China crescendo menos, o preço do minério caiu e a ação acompanhou.' WHERE id = 'cccccccc-0403-0000-0000-000000000403';
UPDATE assets SET reveal_note = 'Cervejaria com margens altíssimas e lucro crescente: subiu mesmo com a economia fraca.' WHERE id = 'cccccccc-0404-0000-0000-000000000404';
UPDATE assets SET reveal_note = 'Banco público que ampliou o crédito barato a pedido do governo; o mercado desconfiou do efeito no lucro.' WHERE id = 'cccccccc-0405-0000-0000-000000000405';
UPDATE assets SET reveal_note = 'Comprado com juros de dois dígitos, valorizou quando a Selic caiu até 7,25%.' WHERE id = 'cccccccc-0406-0000-0000-000000000406';
UPDATE assets SET reveal_note = 'Título longo: sobe muito quando os juros caem, como em 2012, e cai quando eles voltam a subir, como em 2013.' WHERE id = 'cccccccc-0407-0000-0000-000000000407';
UPDATE assets SET reveal_note = 'Rendeu a Selic dia a dia, sem oscilar.' WHERE id = 'cccccccc-0408-0000-0000-000000000408';

-- ------------------------------------------------------------
-- Recessão e Lava Jato (2014–2016)
-- ------------------------------------------------------------
UPDATE competitions SET debrief =
'Em 2015 e 2016 a economia encolheu mais de 3% ao ano. Minério e petróleo desabaram no mercado internacional, e a Operação Lava Jato revelou um esquema de corrupção na Petrobras, que registrou perdas bilionárias. Com a inflação acima de 10% em 2015, a Selic chegou a 14,25%: quem ficou em títulos públicos ganhou bem acima da inflação, com risco baixo. Em 2016, com a troca de governo e a expectativa de juros menores, a bolsa virou e o Ibovespa subiu quase 40% no ano. O que dá para aprender: juros altos tornam a renda fixa muito competitiva, e ações que caem muito numa crise podem se recuperar, mas só para quem aguenta esperar.'
WHERE scenario_title = 'Recessão e Lava Jato (2014–2016)' AND start_year = 2014;

UPDATE assets SET reveal_note = 'Centro da Operação Lava Jato: registrou perdas bilionárias e tinha dívida alta, mas se recuperou com a troca de gestão em 2016.' WHERE id = 'cccccccc-0101-0000-0000-000000000101';
UPDATE assets SET reveal_note = 'O minério de ferro caiu a menos de US$ 40 a tonelada, e em 2015 rompeu-se a barragem da Samarco, em Mariana, empresa que ela controlava junto com a BHP.' WHERE id = 'cccccccc-0102-0000-0000-000000000102';
UPDATE assets SET reveal_note = 'Maior banco privado do país: manteve lucros altos mesmo na recessão.' WHERE id = 'cccccccc-0103-0000-0000-000000000103';
UPDATE assets SET reveal_note = 'Vende cerveja e refrigerante, que as pessoas continuam comprando na crise: oscilou pouco.' WHERE id = 'cccccccc-0104-0000-0000-000000000104';
UPDATE assets SET reveal_note = 'Varejista que apostou no comércio eletrônico: perdeu quase 80% em 2014 e 2015 e, em 2016, foi uma das ações que mais subiram na bolsa.' WHERE id = 'cccccccc-0105-0000-0000-000000000105';
UPDATE assets SET reveal_note = 'Comprado com juros acima de 10%, travou um bom retorno enquanto a economia afundava.' WHERE id = 'cccccccc-0106-0000-0000-000000000106';
UPDATE assets SET reveal_note = 'Pagou a inflação, que passou de 10% em 2015, mais uma taxa fixa.' WHERE id = 'cccccccc-0107-0000-0000-000000000107';
UPDATE assets SET reveal_note = 'Rendeu a Selic, que chegou a 14,25% ao ano, sem oscilar: difícil de bater na recessão.' WHERE id = 'cccccccc-0108-0000-0000-000000000108';

-- ------------------------------------------------------------
-- Juros mínimos e a chegada da pandemia (2017–2020)
-- ------------------------------------------------------------
UPDATE competitions SET debrief =
'A Selic caiu de 13,75% para 2% ao ano, e a renda fixa passou a render pouco: milhões de pessoas foram para a bolsa, que bateu recordes até o começo de 2020. Em janeiro de 2019, o rompimento da barragem de Brumadinho matou 270 pessoas. Em março de 2020, a pandemia derrubou o Ibovespa em cerca de 30% em um único mês; turismo, shoppings e lojas de rua foram os mais atingidos. Com juros baixíssimos e estímulos no mundo todo, a bolsa se recuperou até o fim do ano, mas não para todas as empresas. O que dá para aprender: com juro baixo, ficar só na renda fixa quase não rende, e o mesmo choque pode ajudar um setor e quebrar outro.'
WHERE scenario_title = 'Juros mínimos e a chegada da pandemia (2017–2020)' AND start_year = 2017;

UPDATE assets SET reveal_note = 'Fabricante de motores e equipamentos elétricos que exporta para o mundo todo: cresceu com a automação e a energia renovável.' WHERE id = 'cccccccc-0201-0000-0000-000000000201';
UPDATE assets SET reveal_note = 'Com juros baixos, os bancos ganharam menos no crédito e passaram a disputar clientes com os bancos digitais.' WHERE id = 'cccccccc-0202-0000-0000-000000000202';
UPDATE assets SET reveal_note = 'Responsável pela barragem de Brumadinho, que se rompeu em 2019; mesmo assim, a alta do minério em 2020 fez a ação subir.' WHERE id = 'cccccccc-0203-0000-0000-000000000203';
UPDATE assets SET reveal_note = 'Maior agência de turismo do país: com aeroportos e fronteiras fechados na pandemia, as vendas desapareceram.' WHERE id = 'cccccccc-0204-0000-0000-000000000204';
UPDATE assets SET reveal_note = 'Varejista de moda que cresceu com a economia até 2019; as lojas fechadas em 2020 pesaram.' WHERE id = 'cccccccc-0205-0000-0000-000000000205';
UPDATE assets SET reveal_note = 'Travou juros de cerca de 10% enquanto a Selic caía rumo a 2%.' WHERE id = 'cccccccc-0206-0000-0000-000000000206';
UPDATE assets SET reveal_note = 'Título longo: valorizou muito com a queda dos juros, sobretudo em 2019.' WHERE id = 'cccccccc-0207-0000-0000-000000000207';
UPDATE assets SET reveal_note = 'Acompanhou a Selic, que caiu até 2%: rendimento baixo, mas sem sustos.' WHERE id = 'cccccccc-0208-0000-0000-000000000208';

-- ------------------------------------------------------------
-- Pandemia e a volta da inflação (2020–2023)
-- ------------------------------------------------------------
UPDATE competitions SET debrief =
'A pandemia fechou o comércio de rua e parou os aviões, e as companhias aéreas acumularam prejuízos enormes. As vendas online dispararam em 2020, mas, com a volta da inflação, a Selic subiu de 2% para 13,75%, e as ações de empresas que dependiam de crédito barato despencaram. A renda fixa voltou a render mais de 12% ao ano. Com o petróleo caro depois da pandemia e da guerra na Ucrânia, a Petrobras pagou dividendos recordes, e os grandes bancos lucraram com os juros altos. O que dá para aprender: o mesmo ativo pode ser o melhor investimento de um ano e o pior do ano seguinte. Quando os juros mudam, muda quase tudo.'
WHERE scenario_title = 'Pandemia e a volta da inflação (2020–2023)' AND start_year = 2020;

UPDATE assets SET reveal_note = 'Com o petróleo caro depois da pandemia e da guerra na Ucrânia, lucrou muito e pagou dividendos recordes.' WHERE id = 'cccccccc-0301-0000-0000-000000000301';
UPDATE assets SET reveal_note = 'Disparou com as vendas online em 2020 e perdeu a maior parte do valor quando os juros voltaram a subir.' WHERE id = 'cccccccc-0302-0000-0000-000000000302';
UPDATE assets SET reveal_note = 'Companhia aérea: com aviões parados na pandemia e dívida em dólar, acumulou prejuízos enormes.' WHERE id = 'cccccccc-0303-0000-0000-000000000303';
UPDATE assets SET reveal_note = 'Lucrou com os juros mais altos e pagava bons dividendos, com as ações baratas em relação ao lucro.' WHERE id = 'cccccccc-0304-0000-0000-000000000304';
UPDATE assets SET reveal_note = 'Dobrou de valor em 2020 com as exportações e a energia renovável, e depois oscilou por estar cara em relação ao lucro.' WHERE id = 'cccccccc-0305-0000-0000-000000000305';
UPDATE assets SET reveal_note = 'Comprado com juros baixos em 2020, perdeu valor em 2021, quando a Selic começou a subir, e só se recuperou depois.' WHERE id = 'cccccccc-0306-0000-0000-000000000306';
UPDATE assets SET reveal_note = 'Título muito longo: caiu bastante com a alta dos juros, mesmo pagando a inflação.' WHERE id = 'cccccccc-0307-0000-0000-000000000307';
UPDATE assets SET reveal_note = 'Acompanhou a Selic de 2% a 13,75%: com a alta dos juros, virou um dos investimentos mais seguros e rentáveis do período.' WHERE id = 'cccccccc-0308-0000-0000-000000000308';
