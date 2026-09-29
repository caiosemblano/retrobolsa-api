-- ============================================================
-- V17: quatro módulos novos, com vídeo, resumo e quiz
-- ============================================================
-- Renda fixa na prática, Risco e diversificação, Finanças pessoais e
-- Comportamento do investidor: 3 aulas cada (bbbbbbbb-0009 a 0020),
-- cada uma com 3 perguntas no mesmo formato do V16.
--
-- Vídeos conferidos em 29/09/2026: existem (oEmbed 200) e podem tocar
-- embutidos (playableInEmbed). O canal de cada um está no comentário da aula.
-- ============================================================

INSERT INTO modules (id, title, description, icon, display_order) VALUES
    ('aaaaaaaa-0004-0000-0000-000000000004', 'Renda fixa na prática', 'Tesouro Direto, CDB e poupança: onde fazer o dinheiro render com segurança.', 'landmark', 4),
    ('aaaaaaaa-0005-0000-0000-000000000005', 'Risco e diversificação', 'Por que os investimentos oscilam e como não depender de uma aposta só.', 'shield-check', 5),
    ('aaaaaaaa-0006-0000-0000-000000000006', 'Finanças pessoais', 'Orçamento, reserva de emergência e como fugir dos juros do cartão.', 'wallet', 6),
    ('aaaaaaaa-0007-0000-0000-000000000007', 'Comportamento do investidor', 'Os atalhos da mente que levam a decisões ruins com dinheiro.', 'brain', 7);

-- Aula 9: Tesouro Selic, Prefixado e IPCA+ (vídeo: Me Poupe!)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0009-0000-0000-000000000009', 'aaaaaaaa-0004-0000-0000-000000000004', 'Tesouro Selic, Prefixado e IPCA+',
     'O Tesouro Direto é o jeito de emprestar dinheiro para o governo federal, começando com valores baixos. São três famílias principais: o Tesouro Selic rende a taxa Selic de cada dia e quase não oscila; o Prefixado tem a taxa travada na compra (você sabe quanto vai receber no vencimento, mas o preço sobe e desce no caminho quando os juros mudam); e o IPCA+ paga a inflação do período mais uma taxa fixa, protegendo o poder de compra. Os "Títulos" das rodadas do RetroBolsa são exatamente esses três tipos.',
     12, 1, 'y2sBkIX72-g');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0009-0001-0000-000000000000', 'bbbbbbbb-0009-0000-0000-000000000009', 1,
     'Qual título do Tesouro rende a taxa Selic de cada dia e quase não oscila de preço?',
     'O Tesouro Selic acompanha os juros do dia a dia; por isso é o mais estável e costuma ser usado para a reserva de emergência.'),
    ('ffffffff-0009-0002-0000-000000000000', 'bbbbbbbb-0009-0000-0000-000000000009', 2,
     'Você comprou um Tesouro Prefixado a 10% ao ano. Logo depois, os juros do país sobem para 13%. O que acontece com o preço do seu título antes do vencimento?',
     'Ninguém quer pagar o mesmo por um título de 10% quando os novos pagam 13%, então o preço cai. Levando até o vencimento, você recebe os 10% combinados; quem vende antes pode perder.'),
    ('ffffffff-0009-0003-0000-000000000000', 'bbbbbbbb-0009-0000-0000-000000000009', 3,
     'Qual título protege melhor o poder de compra contra uma inflação alta e inesperada?',
     'O IPCA+ paga a inflação do período mais uma taxa fixa: se a inflação disparar, o rendimento acompanha. No prefixado, uma inflação maior que a esperada come o ganho real.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0009-0001-0001-000000000000', 'ffffffff-0009-0001-0000-000000000000', 1, 'Tesouro Prefixado', FALSE),
    ('ffffffff-0009-0001-0002-000000000000', 'ffffffff-0009-0001-0000-000000000000', 2, 'Tesouro IPCA+', FALSE),
    ('ffffffff-0009-0001-0003-000000000000', 'ffffffff-0009-0001-0000-000000000000', 3, 'Tesouro Selic', TRUE),
    ('ffffffff-0009-0001-0004-000000000000', 'ffffffff-0009-0001-0000-000000000000', 4, 'Nenhum deles', FALSE),
    ('ffffffff-0009-0002-0001-000000000000', 'ffffffff-0009-0002-0000-000000000000', 1, 'Sobe', FALSE),
    ('ffffffff-0009-0002-0002-000000000000', 'ffffffff-0009-0002-0000-000000000000', 2, 'Cai', TRUE),
    ('ffffffff-0009-0002-0003-000000000000', 'ffffffff-0009-0002-0000-000000000000', 3, 'Não muda', FALSE),
    ('ffffffff-0009-0002-0004-000000000000', 'ffffffff-0009-0002-0000-000000000000', 4, 'O título é cancelado', FALSE),
    ('ffffffff-0009-0003-0001-000000000000', 'ffffffff-0009-0003-0000-000000000000', 1, 'Tesouro IPCA+', TRUE),
    ('ffffffff-0009-0003-0002-000000000000', 'ffffffff-0009-0003-0000-000000000000', 2, 'Tesouro Prefixado', FALSE),
    ('ffffffff-0009-0003-0003-000000000000', 'ffffffff-0009-0003-0000-000000000000', 3, 'Poupança', FALSE),
    ('ffffffff-0009-0003-0004-000000000000', 'ffffffff-0009-0003-0000-000000000000', 4, 'Dinheiro guardado em casa', FALSE);

-- Aula 10: CDB e a garantia do FGC (vídeo: Primo Pobre)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0010-0000-0000-000000000010', 'aaaaaaaa-0004-0000-0000-000000000004', 'CDB e a garantia do FGC',
     'O CDB é um empréstimo que você faz a um banco: ele usa o dinheiro e devolve com juros, em geral uma porcentagem do CDI (um CDB de 100% do CDI rende o mesmo que o CDI). Se o banco quebrar, o FGC (Fundo Garantidor de Créditos) devolve o dinheiro até um limite por pessoa em cada instituição. Bancos menores costumam pagar mais porque o risco é maior, mas, dentro do limite do FGC, esse risco fica coberto. Poupança, LCI e LCA também têm a garantia; ações e fundos, não.',
     5, 2, '6bc5vM_XwfA');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0010-0001-0000-000000000000', 'bbbbbbbb-0010-0000-0000-000000000010', 1,
     'Ao investir num CDB, você está:',
     'O CDB é um título de dívida do banco: ele usa o seu dinheiro e devolve com os juros combinados. Quem empresta para o governo é quem compra títulos do Tesouro.'),
    ('ffffffff-0010-0002-0000-000000000000', 'bbbbbbbb-0010-0000-0000-000000000010', 2,
     'Um CDB paga 100% do CDI, e o CDI rendeu 12% no ano. Quanto o CDB rendeu, antes do imposto?',
     '100% do CDI é render o mesmo que o CDI: 12%. Um CDB de 110% do CDI renderia 13,2%.'),
    ('ffffffff-0010-0003-0000-000000000000', 'bbbbbbbb-0010-0000-0000-000000000010', 3,
     'O que o FGC faz?',
     'O Fundo Garantidor de Créditos cobre investimentos como CDB, LCI, LCA e poupança até um limite por pessoa em cada instituição. Ações e fundos não têm essa garantia.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0010-0001-0001-000000000000', 'ffffffff-0010-0001-0000-000000000000', 1, 'Comprando uma parte do banco', FALSE),
    ('ffffffff-0010-0001-0002-000000000000', 'ffffffff-0010-0001-0000-000000000000', 2, 'Emprestando dinheiro para o governo', FALSE),
    ('ffffffff-0010-0001-0003-000000000000', 'ffffffff-0010-0001-0000-000000000000', 3, 'Apostando na bolsa', FALSE),
    ('ffffffff-0010-0001-0004-000000000000', 'ffffffff-0010-0001-0000-000000000000', 4, 'Emprestando dinheiro para o banco', TRUE),
    ('ffffffff-0010-0002-0001-000000000000', 'ffffffff-0010-0002-0000-000000000000', 1, '100%', FALSE),
    ('ffffffff-0010-0002-0002-000000000000', 'ffffffff-0010-0002-0000-000000000000', 2, '1,2%', FALSE),
    ('ffffffff-0010-0002-0003-000000000000', 'ffffffff-0010-0002-0000-000000000000', 3, '112%', FALSE),
    ('ffffffff-0010-0002-0004-000000000000', 'ffffffff-0010-0002-0000-000000000000', 4, '12%', TRUE),
    ('ffffffff-0010-0003-0001-000000000000', 'ffffffff-0010-0003-0000-000000000000', 1, 'Garante lucro nas ações', FALSE),
    ('ffffffff-0010-0003-0002-000000000000', 'ffffffff-0010-0003-0000-000000000000', 2, 'Devolve o dinheiro de CDBs e poupança, até um limite, se o banco quebrar', TRUE),
    ('ffffffff-0010-0003-0003-000000000000', 'ffffffff-0010-0003-0000-000000000000', 3, 'Define a taxa Selic', FALSE),
    ('ffffffff-0010-0003-0004-000000000000', 'ffffffff-0010-0003-0000-000000000000', 4, 'Cobra imposto sobre os investimentos', FALSE);

-- Aula 11: Poupança x CDI: quanto se deixa na mesa (vídeo: Ela Investe)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0011-0000-0000-000000000011', 'aaaaaaaa-0004-0000-0000-000000000004', 'Poupança x CDI: quanto se deixa na mesa',
     'A poupança é simples e não tem imposto de renda, mas rende pouco: 0,5% ao mês mais a TR quando a Selic está acima de 8,5% ao ano, ou 70% da Selic quando está em 8,5% ou menos. Um CDB de 100% do CDI ou o Tesouro Selic rendem perto do CDI e, mesmo pagando imposto, costumam render mais. No resultado das rodadas do RetroBolsa dá para ver a diferença: de 2011 a 2013, a poupança rendeu 20,6%, contra 30,7% do CDI.',
     17, 3, 'T2zYwfiF6Po');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0011-0001-0000-000000000000', 'bbbbbbbb-0011-0000-0000-000000000011', 1,
     'Com a Selic acima de 8,5% ao ano, quanto rende a poupança?',
     'Essa é a regra para depósitos feitos desde maio de 2012. Com a Selic em 8,5% ou menos, a poupança passa a render 70% da Selic mais a TR.'),
    ('ffffffff-0011-0002-0000-000000000000', 'bbbbbbbb-0011-0000-0000-000000000011', 2,
     'De 2011 a 2013, o CDI rendeu cerca de 30,7% e a poupança, 20,6%. Com R$ 10.000, quanto a mais você teria no CDI?',
     'R$ 10.000 viraram cerca de R$ 13.070 no CDI e R$ 12.060 na poupança: uma diferença de cerca de R$ 1.000 em três anos.'),
    ('ffffffff-0011-0003-0000-000000000000', 'bbbbbbbb-0011-0000-0000-000000000011', 3,
     'Qual é uma vantagem da poupança sobre um CDB?',
     'A poupança é isenta de imposto de renda e simples de usar. Mesmo assim, um CDB de 100% do CDI costuma render mais, já descontado o imposto.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0011-0001-0001-000000000000', 'ffffffff-0011-0001-0000-000000000000', 1, '100% do CDI', FALSE),
    ('ffffffff-0011-0001-0002-000000000000', 'ffffffff-0011-0001-0000-000000000000', 2, '0,5% ao mês mais a TR', TRUE),
    ('ffffffff-0011-0001-0003-000000000000', 'ffffffff-0011-0001-0000-000000000000', 3, '70% da inflação', FALSE),
    ('ffffffff-0011-0001-0004-000000000000', 'ffffffff-0011-0001-0000-000000000000', 4, 'A Selic inteira', FALSE),
    ('ffffffff-0011-0002-0001-000000000000', 'ffffffff-0011-0002-0000-000000000000', 1, 'Cerca de R$ 1.000', TRUE),
    ('ffffffff-0011-0002-0002-000000000000', 'ffffffff-0011-0002-0000-000000000000', 2, 'R$ 307', FALSE),
    ('ffffffff-0011-0002-0003-000000000000', 'ffffffff-0011-0002-0000-000000000000', 3, 'R$ 10', FALSE),
    ('ffffffff-0011-0002-0004-000000000000', 'ffffffff-0011-0002-0000-000000000000', 4, 'Nada, os dois rendem igual', FALSE),
    ('ffffffff-0011-0003-0001-000000000000', 'ffffffff-0011-0003-0000-000000000000', 1, 'Rende sempre mais', FALSE),
    ('ffffffff-0011-0003-0002-000000000000', 'ffffffff-0011-0003-0000-000000000000', 2, 'Tem garantia sem limite', FALSE),
    ('ffffffff-0011-0003-0003-000000000000', 'ffffffff-0011-0003-0000-000000000000', 3, 'Não tem imposto de renda', TRUE),
    ('ffffffff-0011-0003-0004-000000000000', 'ffffffff-0011-0003-0000-000000000000', 4, 'Acompanha a inflação', FALSE);

-- Aula 12: O que é risco e volatilidade (vídeo: Bradesco Asset Management)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0012-0000-0000-000000000012', 'aaaaaaaa-0005-0000-0000-000000000005', 'O que é risco e volatilidade',
     'Volatilidade é o quanto o preço de um investimento sobe e desce. Ações são voláteis: a bolsa brasileira caiu mais de 40% em 2008 e subiu mais de 80% em 2009. Títulos como o Tesouro Selic quase não oscilam. Risco é a chance de o resultado sair diferente do esperado, inclusive de perder dinheiro, e mais retorno esperado costuma vir junto com mais risco. Quem vai precisar do dinheiro logo não pode esperar a recuperação de uma queda.',
     4, 1, '-yyjmU4sd3Y');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0012-0001-0000-000000000000', 'bbbbbbbb-0012-0000-0000-000000000012', 1,
     'O que é volatilidade?',
     'Volatilidade mede a oscilação do preço. Um investimento muito volátil pode estar bem acima ou bem abaixo do que você pagou em poucos meses.'),
    ('ffffffff-0012-0002-0000-000000000000', 'bbbbbbbb-0012-0000-0000-000000000012', 2,
     'Qual destes costuma ser o mais volátil?',
     'As ações de uma empresa sobem e descem com os resultados dela e com o humor do mercado. Tesouro Selic, poupança e CDB quase não oscilam.'),
    ('ffffffff-0012-0003-0000-000000000000', 'bbbbbbbb-0012-0000-0000-000000000012', 3,
     'Em geral, um investimento com mais retorno esperado:',
     'Ninguém aceitaria correr mais risco sem a chance de ganhar mais. Desconfie de quem promete retorno alto e sem risco: é um sinal clássico de golpe.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0012-0001-0001-000000000000', 'ffffffff-0012-0001-0000-000000000000', 1, 'O quanto o preço de um investimento sobe e desce', TRUE),
    ('ffffffff-0012-0001-0002-000000000000', 'ffffffff-0012-0001-0000-000000000000', 2, 'O imposto sobre o lucro', FALSE),
    ('ffffffff-0012-0001-0003-000000000000', 'ffffffff-0012-0001-0000-000000000000', 3, 'A taxa de administração', FALSE),
    ('ffffffff-0012-0001-0004-000000000000', 'ffffffff-0012-0001-0000-000000000000', 4, 'O prazo do investimento', FALSE),
    ('ffffffff-0012-0002-0001-000000000000', 'ffffffff-0012-0002-0000-000000000000', 1, 'Tesouro Selic', FALSE),
    ('ffffffff-0012-0002-0002-000000000000', 'ffffffff-0012-0002-0000-000000000000', 2, 'Poupança', FALSE),
    ('ffffffff-0012-0002-0003-000000000000', 'ffffffff-0012-0002-0000-000000000000', 3, 'CDB de liquidez diária', FALSE),
    ('ffffffff-0012-0002-0004-000000000000', 'ffffffff-0012-0002-0000-000000000000', 4, 'Ações de uma única empresa', TRUE),
    ('ffffffff-0012-0003-0001-000000000000', 'ffffffff-0012-0003-0000-000000000000', 1, 'Tem menos risco', FALSE),
    ('ffffffff-0012-0003-0002-000000000000', 'ffffffff-0012-0003-0000-000000000000', 2, 'Não tem risco nenhum', FALSE),
    ('ffffffff-0012-0003-0003-000000000000', 'ffffffff-0012-0003-0000-000000000000', 3, 'Tem sempre a garantia do FGC', FALSE),
    ('ffffffff-0012-0003-0004-000000000000', 'ffffffff-0012-0003-0000-000000000000', 4, 'Tem mais risco', TRUE);

-- Aula 13: Por que diversificar (vídeo: José Kobori)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0013-0000-0000-000000000013', 'aaaaaaaa-0005-0000-0000-000000000005', 'Por que diversificar',
     'Diversificar é não colocar todo o dinheiro num lugar só. Como ninguém sabe qual ativo vai render mais, espalhar entre empresas, setores e tipos de investimento faz o erro em um ser compensado pelo acerto em outro. Numa rodada do RetroBolsa, quem pôs tudo numa empresa que quebrou perdeu quase tudo; quem dividiu entre ações e títulos sentiu bem menos a queda. Diversificar não garante ganho, mas diminui muito a chance de um desastre.',
     7, 2, 'HgPPig1HFc8');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0013-0001-0000-000000000000', 'bbbbbbbb-0013-0000-0000-000000000013', 1,
     'O que é diversificar?',
     'Diversificar é dividir o dinheiro entre ativos diferentes, para que o resultado não dependa de uma aposta só.'),
    ('ffffffff-0013-0002-0000-000000000000', 'bbbbbbbb-0013-0000-0000-000000000013', 2,
     'Por que diversificar ajuda?',
     'Ninguém sabe qual ativo vai render mais. Diversificar não garante ganho nem elimina o risco, mas evita que um único erro derrube a carteira inteira.'),
    ('ffffffff-0013-0003-0000-000000000000', 'bbbbbbbb-0013-0000-0000-000000000013', 3,
     'Qual carteira é mais diversificada?',
     'Cinco bancos são cinco ativos, mas do mesmo setor: tendem a sofrer juntos. Misturar setores e tipos de investimento é que diversifica de verdade.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0013-0001-0001-000000000000', 'ffffffff-0013-0001-0000-000000000000', 1, 'Colocar tudo no investimento que mais rendeu no ano passado', FALSE),
    ('ffffffff-0013-0001-0002-000000000000', 'ffffffff-0013-0001-0000-000000000000', 2, 'Espalhar o dinheiro entre vários ativos diferentes', TRUE),
    ('ffffffff-0013-0001-0003-000000000000', 'ffffffff-0013-0001-0000-000000000000', 3, 'Trocar de investimento todo mês', FALSE),
    ('ffffffff-0013-0001-0004-000000000000', 'ffffffff-0013-0001-0000-000000000000', 4, 'Investir só em renda fixa', FALSE),
    ('ffffffff-0013-0002-0001-000000000000', 'ffffffff-0013-0002-0000-000000000000', 1, 'Garante lucro', FALSE),
    ('ffffffff-0013-0002-0002-000000000000', 'ffffffff-0013-0002-0000-000000000000', 2, 'Elimina todo o risco', FALSE),
    ('ffffffff-0013-0002-0003-000000000000', 'ffffffff-0013-0002-0000-000000000000', 3, 'Um ativo que vai mal pode ser compensado por outro que vai bem', TRUE),
    ('ffffffff-0013-0002-0004-000000000000', 'ffffffff-0013-0002-0000-000000000000', 4, 'Diminui o imposto', FALSE),
    ('ffffffff-0013-0003-0001-000000000000', 'ffffffff-0013-0003-0000-000000000000', 1, 'Ações de cinco bancos', FALSE),
    ('ffffffff-0013-0003-0002-000000000000', 'ffffffff-0013-0003-0000-000000000000', 2, 'Ações de setores diferentes e títulos públicos', TRUE),
    ('ffffffff-0013-0003-0003-000000000000', 'ffffffff-0013-0003-0000-000000000000', 3, 'Ações de uma empresa de petróleo', FALSE),
    ('ffffffff-0013-0003-0004-000000000000', 'ffffffff-0013-0003-0000-000000000000', 4, 'Ações de duas mineradoras', FALSE);

-- Aula 14: Horizonte de investimento: curto x longo prazo (vídeo: Gustavo Cerbasi)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0014-0000-0000-000000000014', 'aaaaaaaa-0005-0000-0000-000000000005', 'Horizonte de investimento: curto x longo prazo',
     'Horizonte é o tempo que o dinheiro pode ficar investido sem ser usado. Para prazos curtos, como uma viagem no fim do ano, o melhor é a renda fixa que quase não oscila. Para prazos longos, como a aposentadoria, dá para aceitar mais volatilidade, porque há tempo para esperar a recuperação de uma queda, embora nada seja garantido. O erro clássico é colocar em ações o dinheiro que vai ser preciso logo e ter de vender justamente na baixa.',
     4, 3, 'E-pl2CVkTmA');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0014-0001-0000-000000000000', 'bbbbbbbb-0014-0000-0000-000000000014', 1,
     'Você vai precisar do dinheiro daqui a 6 meses para a matrícula da faculdade. Qual é a escolha mais adequada?',
     'Para prazo curto, o importante é não correr o risco de o valor cair justo quando você precisar dele. Ações e títulos longos podem estar em baixa na hora do resgate.'),
    ('ffffffff-0014-0002-0000-000000000000', 'bbbbbbbb-0014-0000-0000-000000000014', 2,
     'Por que quem investe para daqui a 20 anos pode ter mais ações do que quem precisa do dinheiro no ano que vem?',
     'Com prazo longo dá para esperar uma crise passar sem vender na baixa. Mesmo assim, ações podem cair por muito tempo: diversificar continua importante.'),
    ('ffffffff-0014-0003-0000-000000000000', 'bbbbbbbb-0014-0000-0000-000000000014', 3,
     'O que é horizonte de investimento?',
     'É o prazo até você precisar do dinheiro. Ele ajuda a escolher quanto risco dá para correr.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0014-0001-0001-000000000000', 'ffffffff-0014-0001-0000-000000000000', 1, 'Ações de uma empresa promissora', FALSE),
    ('ffffffff-0014-0001-0002-000000000000', 'ffffffff-0014-0001-0000-000000000000', 2, 'Tesouro IPCA+ com vencimento em 2045', FALSE),
    ('ffffffff-0014-0001-0003-000000000000', 'ffffffff-0014-0001-0000-000000000000', 3, 'Tesouro Selic ou CDB de liquidez diária', TRUE),
    ('ffffffff-0014-0001-0004-000000000000', 'ffffffff-0014-0001-0000-000000000000', 4, 'Criptomoedas', FALSE),
    ('ffffffff-0014-0002-0001-000000000000', 'ffffffff-0014-0002-0000-000000000000', 1, 'Porque ações não caem no longo prazo', FALSE),
    ('ffffffff-0014-0002-0002-000000000000', 'ffffffff-0014-0002-0000-000000000000', 2, 'Porque o imposto é menor', FALSE),
    ('ffffffff-0014-0002-0003-000000000000', 'ffffffff-0014-0002-0000-000000000000', 3, 'Porque a renda fixa acaba', FALSE),
    ('ffffffff-0014-0002-0004-000000000000', 'ffffffff-0014-0002-0000-000000000000', 4, 'Porque tem tempo para esperar a recuperação de uma queda', TRUE),
    ('ffffffff-0014-0003-0001-000000000000', 'ffffffff-0014-0003-0000-000000000000', 1, 'O tempo que o dinheiro pode ficar investido sem ser usado', TRUE),
    ('ffffffff-0014-0003-0002-000000000000', 'ffffffff-0014-0003-0000-000000000000', 2, 'A rentabilidade máxima possível', FALSE),
    ('ffffffff-0014-0003-0003-000000000000', 'ffffffff-0014-0003-0000-000000000000', 3, 'O valor mínimo para investir', FALSE),
    ('ffffffff-0014-0003-0004-000000000000', 'ffffffff-0014-0003-0000-000000000000', 4, 'O nome de um título do Tesouro', FALSE);

-- Aula 15: Orçamento: para onde vai o seu dinheiro (vídeo: Banco Central do Brasil)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0015-0000-0000-000000000015', 'aaaaaaaa-0006-0000-0000-000000000006', 'Orçamento: para onde vai o seu dinheiro',
     'Orçamento é anotar quanto entra e para onde vai cada real. Sem anotar, a gente confia na memória, que esquece os gastos pequenos do dia a dia, e o "orçamento de cabeça" não fecha no fim do mês. Um jeito simples de começar é dividir a renda entre necessidades, desejos e poupança, como na regra 50-30-20, e guardar a parte da poupança assim que o dinheiro entra, e não com o que sobrar no fim do mês.',
     5, 1, 'AavioxdeiLY');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0015-0001-0000-000000000000', 'bbbbbbbb-0015-0000-0000-000000000015', 1,
     'Qual é o primeiro passo de um orçamento?',
     'Só dá para decidir onde economizar depois de saber para onde o dinheiro vai de verdade.'),
    ('ffffffff-0015-0002-0000-000000000000', 'bbbbbbbb-0015-0000-0000-000000000015', 2,
     'Por que o "orçamento de cabeça" costuma falhar?',
     'Lanches, transporte, aplicativos: gastos pequenos e frequentes somam muito no fim do mês e somem da memória. Anotar mostra para onde o dinheiro vai de fato.'),
    ('ffffffff-0015-0003-0000-000000000000', 'bbbbbbbb-0015-0000-0000-000000000015', 3,
     'Na regra 50-30-20, com uma renda de R$ 1.000, quanto vai para poupar e investir?',
     '50% para necessidades (R$ 500), 30% para desejos (R$ 300) e 20% para poupar e investir (R$ 200). É um ponto de partida; o importante é guardar assim que o dinheiro entra.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0015-0001-0001-000000000000', 'ffffffff-0015-0001-0000-000000000000', 1, 'Anotar quanto entra e para onde vai cada gasto', TRUE),
    ('ffffffff-0015-0001-0002-000000000000', 'ffffffff-0015-0001-0000-000000000000', 2, 'Cortar todos os gastos com lazer', FALSE),
    ('ffffffff-0015-0001-0003-000000000000', 'ffffffff-0015-0001-0000-000000000000', 3, 'Investir em ações', FALSE),
    ('ffffffff-0015-0001-0004-000000000000', 'ffffffff-0015-0001-0000-000000000000', 4, 'Pedir um cartão de crédito', FALSE),
    ('ffffffff-0015-0002-0001-000000000000', 'ffffffff-0015-0002-0000-000000000000', 1, 'Porque a conta é difícil demais', FALSE),
    ('ffffffff-0015-0002-0002-000000000000', 'ffffffff-0015-0002-0000-000000000000', 2, 'Porque a memória esquece os gastos pequenos do dia a dia', TRUE),
    ('ffffffff-0015-0002-0003-000000000000', 'ffffffff-0015-0002-0000-000000000000', 3, 'Porque os bancos escondem os gastos', FALSE),
    ('ffffffff-0015-0002-0004-000000000000', 'ffffffff-0015-0002-0000-000000000000', 4, 'Porque só funciona para quem ganha muito', FALSE),
    ('ffffffff-0015-0003-0001-000000000000', 'ffffffff-0015-0003-0000-000000000000', 1, 'R$ 500', FALSE),
    ('ffffffff-0015-0003-0002-000000000000', 'ffffffff-0015-0003-0000-000000000000', 2, 'R$ 300', FALSE),
    ('ffffffff-0015-0003-0003-000000000000', 'ffffffff-0015-0003-0000-000000000000', 3, 'R$ 20', FALSE),
    ('ffffffff-0015-0003-0004-000000000000', 'ffffffff-0015-0003-0000-000000000000', 4, 'R$ 200', TRUE);

-- Aula 16: Reserva de emergência (vídeo: Banco Central do Brasil)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0016-0000-0000-000000000016', 'aaaaaaaa-0006-0000-0000-000000000006', 'Reserva de emergência',
     'A reserva de emergência é um dinheiro guardado para imprevistos: perder o emprego, um problema de saúde, um conserto urgente. Uma referência comum é juntar de 3 a 6 meses dos gastos mensais. Ela precisa ficar num investimento seguro e que se resgate em qualquer dia, como o Tesouro Selic ou um CDB com liquidez diária. Com a reserva formada, uma emergência não obriga a vender ações na baixa nem a pegar um empréstimo caro.',
     6, 2, 'eesZ68jPuag');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0016-0001-0000-000000000000', 'bbbbbbbb-0016-0000-0000-000000000016', 1,
     'Para que serve a reserva de emergência?',
     'A reserva existe para que um imprevisto não vire dívida cara nem obrigue a vender investimentos na hora errada.'),
    ('ffffffff-0016-0002-0000-000000000000', 'bbbbbbbb-0016-0000-0000-000000000016', 2,
     'Onde é mais adequado guardar a reserva de emergência?',
     'A reserva precisa estar disponível em qualquer dia e não pode perder valor justamente na hora do aperto.'),
    ('ffffffff-0016-0003-0000-000000000000', 'bbbbbbbb-0016-0000-0000-000000000016', 3,
     'Uma pessoa gasta R$ 2.000 por mês. Qual seria uma reserva de 6 meses?',
     '6 × R$ 2.000 = R$ 12.000. A referência comum é de 3 a 6 meses de gastos; quem tem renda instável costuma buscar mais.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0016-0001-0001-000000000000', 'ffffffff-0016-0001-0000-000000000000', 1, 'Para comprar ações quando a bolsa cai', FALSE),
    ('ffffffff-0016-0001-0002-000000000000', 'ffffffff-0016-0001-0000-000000000000', 2, 'Para pagar imprevistos sem se endividar', TRUE),
    ('ffffffff-0016-0001-0003-000000000000', 'ffffffff-0016-0001-0000-000000000000', 3, 'Para viajar nas férias', FALSE),
    ('ffffffff-0016-0001-0004-000000000000', 'ffffffff-0016-0001-0000-000000000000', 4, 'Para pagar o mínimo da fatura do cartão', FALSE),
    ('ffffffff-0016-0002-0001-000000000000', 'ffffffff-0016-0002-0000-000000000000', 1, 'Em ações', FALSE),
    ('ffffffff-0016-0002-0002-000000000000', 'ffffffff-0016-0002-0000-000000000000', 2, 'Num Tesouro Prefixado de 10 anos', FALSE),
    ('ffffffff-0016-0002-0003-000000000000', 'ffffffff-0016-0002-0000-000000000000', 3, 'Tesouro Selic ou CDB de liquidez diária', TRUE),
    ('ffffffff-0016-0002-0004-000000000000', 'ffffffff-0016-0002-0000-000000000000', 4, 'Em criptomoedas', FALSE),
    ('ffffffff-0016-0003-0001-000000000000', 'ffffffff-0016-0003-0000-000000000000', 1, 'R$ 12.000', TRUE),
    ('ffffffff-0016-0003-0002-000000000000', 'ffffffff-0016-0003-0000-000000000000', 2, 'R$ 2.000', FALSE),
    ('ffffffff-0016-0003-0003-000000000000', 'ffffffff-0016-0003-0000-000000000000', 3, 'R$ 6.000', FALSE),
    ('ffffffff-0016-0003-0004-000000000000', 'ffffffff-0016-0003-0000-000000000000', 4, 'R$ 24.000', FALSE);

-- Aula 17: Juros do cartão e do cheque especial (vídeo: Banco Central do Brasil)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0017-0000-0000-000000000017', 'aaaaaaaa-0006-0000-0000-000000000006', 'Juros do cartão e do cheque especial',
     'O rotativo do cartão (o que acontece quando se paga só parte da fatura) e o cheque especial estão entre os créditos mais caros do país, com juros que passam de 100% ao ano. Por isso a regra de ouro é pagar a fatura inteira. Se não der, é melhor trocar a dívida por uma mais barata, como o parcelamento da fatura ou um empréstimo pessoal, do que deixar no rotativo. Uma dívida a mais de 100% ao ano cresce muito mais rápido do que qualquer investimento comum consegue render.',
     5, 3, '8_dnQeaC5mI');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0017-0001-0000-000000000000', 'bbbbbbbb-0017-0000-0000-000000000017', 1,
     'O que é o rotativo do cartão?',
     'O que fica sem pagar na fatura vira dívida no rotativo, com juros altíssimos.'),
    ('ffffffff-0017-0002-0000-000000000000', 'bbbbbbbb-0017-0000-0000-000000000017', 2,
     'Não deu para pagar a fatura inteira. Qual costuma ser a melhor saída?',
     'Rotativo e cheque especial estão entre os créditos mais caros. Trocar por uma dívida com juros menores reduz o estrago.'),
    ('ffffffff-0017-0003-0000-000000000000', 'bbbbbbbb-0017-0000-0000-000000000017', 3,
     'Uma dívida cobra juros de mais de 100% ao ano, e um investimento rende 12% ao ano. O que fazer com o dinheiro disponível?',
     'Quitar uma dívida de 100% ao ano equivale a ganhar 100% sem risco. Nenhum investimento comum chega perto disso.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0017-0001-0001-000000000000', 'ffffffff-0017-0001-0000-000000000000', 1, 'Um cartão novo que o banco envia', FALSE),
    ('ffffffff-0017-0001-0002-000000000000', 'ffffffff-0017-0001-0000-000000000000', 2, 'O crédito que entra quando você paga só parte da fatura', TRUE),
    ('ffffffff-0017-0001-0003-000000000000', 'ffffffff-0017-0001-0000-000000000000', 3, 'O limite do cartão', FALSE),
    ('ffffffff-0017-0001-0004-000000000000', 'ffffffff-0017-0001-0000-000000000000', 4, 'O dinheiro de volta das compras', FALSE),
    ('ffffffff-0017-0002-0001-000000000000', 'ffffffff-0017-0002-0000-000000000000', 1, 'Pagar o mínimo e deixar o resto no rotativo por meses', FALSE),
    ('ffffffff-0017-0002-0002-000000000000', 'ffffffff-0017-0002-0000-000000000000', 2, 'Usar o cheque especial para pagar o cartão', FALSE),
    ('ffffffff-0017-0002-0003-000000000000', 'ffffffff-0017-0002-0000-000000000000', 3, 'Trocar a dívida por uma mais barata, como o parcelamento da fatura ou um empréstimo pessoal', TRUE),
    ('ffffffff-0017-0002-0004-000000000000', 'ffffffff-0017-0002-0000-000000000000', 4, 'Ignorar a fatura', FALSE),
    ('ffffffff-0017-0003-0001-000000000000', 'ffffffff-0017-0003-0000-000000000000', 1, 'Quitar a dívida primeiro', TRUE),
    ('ffffffff-0017-0003-0002-000000000000', 'ffffffff-0017-0003-0000-000000000000', 2, 'Investir e deixar a dívida para depois', FALSE),
    ('ffffffff-0017-0003-0003-000000000000', 'ffffffff-0017-0003-0000-000000000000', 3, 'Dividir meio a meio', FALSE),
    ('ffffffff-0017-0003-0004-000000000000', 'ffffffff-0017-0003-0000-000000000000', 4, 'Esperar os juros baixarem sozinhos', FALSE);

-- Aula 18: Aversão à perda (vídeo: T2 Educação)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0018-0000-0000-000000000018', 'aaaaaaaa-0007-0000-0000-000000000007', 'Aversão à perda',
     'Estudos de finanças comportamentais mostram que a dor de perder dinheiro pesa mais que a alegria de ganhar a mesma quantia; em vários experimentos, cerca de duas vezes mais. Por isso muita gente segura uma ação que só cai, esperando "voltar ao preço que paguei", e vende depressa as que sobem, para garantir o lucro. O preço que você pagou não muda o futuro da empresa: a pergunta útil é se, hoje, você compraria aquela ação.',
     5, 1, 'dZK6Y6y7tmw');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0018-0001-0000-000000000000', 'bbbbbbbb-0018-0000-0000-000000000018', 1,
     'O que é aversão à perda?',
     'É um viés estudado pelas finanças comportamentais: perdas pesam mais que ganhos do mesmo tamanho, e isso distorce as decisões.'),
    ('ffffffff-0018-0002-0000-000000000000', 'bbbbbbbb-0018-0000-0000-000000000018', 2,
     'Você comprou uma ação a R$ 20, e ela está em R$ 12. Qual é a pergunta mais útil a fazer?',
     'O preço que você pagou não muda o futuro da empresa. Segurar a ação só para "não realizar o prejuízo" é a aversão à perda na prática.'),
    ('ffffffff-0018-0003-0000-000000000000', 'bbbbbbbb-0018-0000-0000-000000000018', 3,
     'Qual comportamento mostra aversão à perda?',
     'Vender os ganhos cedo e segurar as perdas por tempo demais é o padrão clássico: evita a dor de assumir o prejuízo, mas costuma piorar o resultado.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0018-0001-0001-000000000000', 'ffffffff-0018-0001-0000-000000000000', 1, 'Não gostar de investir', FALSE),
    ('ffffffff-0018-0001-0002-000000000000', 'ffffffff-0018-0001-0000-000000000000', 2, 'Sentir a dor de perder mais do que a alegria de ganhar o mesmo valor', TRUE),
    ('ffffffff-0018-0001-0003-000000000000', 'ffffffff-0018-0001-0000-000000000000', 3, 'Ter medo de banco', FALSE),
    ('ffffffff-0018-0001-0004-000000000000', 'ffffffff-0018-0001-0000-000000000000', 4, 'Preferir ações a títulos', FALSE),
    ('ffffffff-0018-0002-0001-000000000000', 'ffffffff-0018-0002-0000-000000000000', 1, '"Quando ela volta para R$ 20?"', FALSE),
    ('ffffffff-0018-0002-0002-000000000000', 'ffffffff-0018-0002-0000-000000000000', 2, '"Quanto eu já perdi?"', FALSE),
    ('ffffffff-0018-0002-0003-000000000000', 'ffffffff-0018-0002-0000-000000000000', 3, '"Quem me indicou essa ação?"', FALSE),
    ('ffffffff-0018-0002-0004-000000000000', 'ffffffff-0018-0002-0000-000000000000', 4, '"Hoje, com o que eu sei, eu compraria essa ação?"', TRUE),
    ('ffffffff-0018-0003-0001-000000000000', 'ffffffff-0018-0003-0000-000000000000', 1, 'Vender rápido as ações que sobem e segurar as que só caem', TRUE),
    ('ffffffff-0018-0003-0002-000000000000', 'ffffffff-0018-0003-0000-000000000000', 2, 'Diversificar a carteira', FALSE),
    ('ffffffff-0018-0003-0003-000000000000', 'ffffffff-0018-0003-0000-000000000000', 3, 'Ter uma reserva de emergência', FALSE),
    ('ffffffff-0018-0003-0004-000000000000', 'ffffffff-0018-0003-0000-000000000000', 4, 'Comparar a carteira com o CDI', FALSE);

-- Aula 19: Efeito manada (vídeo: Empiricus)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0019-0000-0000-000000000019', 'aaaaaaaa-0007-0000-0000-000000000007', 'Efeito manada',
     'Efeito manada é fazer o que todo mundo está fazendo, sem analisar por conta própria. Na bolsa, ele aparece quando uma ação sobe muito porque todos estão comprando (e muita gente compra no topo, com medo de ficar de fora) ou quando o pânico faz todos venderem de uma vez. Nas bolhas, como a das empresas de internet no ano 2000, a manada leva os preços muito além do que as empresas valem. Antes de seguir a multidão, olhe os números: lucro, P/L, dívida.',
     3, 2, 'WVx4xoWZtqQ');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0019-0001-0000-000000000000', 'bbbbbbbb-0019-0000-0000-000000000019', 1,
     'O que é efeito manada?',
     'É decidir pelo comportamento dos outros, e não pela própria análise. Na bolsa, isso amplia tanto as altas quanto as quedas.'),
    ('ffffffff-0019-0002-0000-000000000000', 'bbbbbbbb-0019-0000-0000-000000000019', 2,
     'Uma ação subiu 300% em poucos meses, e todo mundo nas redes está comprando. Qual é a atitude mais prudente?',
     'Uma alta forte pode ter motivo, ou pode ser só a manada. Os números da empresa ajudam a separar uma coisa da outra.'),
    ('ffffffff-0019-0003-0000-000000000000', 'bbbbbbbb-0019-0000-0000-000000000019', 3,
     'O que costuma acontecer numa bolha?',
     'Na bolha da internet, em 2000, muitas empresas sem lucro valiam fortunas; quando a manada mudou de direção, várias ações perderam mais de 70%.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0019-0001-0001-000000000000', 'ffffffff-0019-0001-0000-000000000000', 1, 'Investir em empresas do agronegócio', FALSE),
    ('ffffffff-0019-0001-0002-000000000000', 'ffffffff-0019-0001-0000-000000000000', 2, 'Diversificar entre muitos ativos', FALSE),
    ('ffffffff-0019-0001-0003-000000000000', 'ffffffff-0019-0001-0000-000000000000', 3, 'Fazer o que todo mundo está fazendo, sem analisar por conta própria', TRUE),
    ('ffffffff-0019-0001-0004-000000000000', 'ffffffff-0019-0001-0000-000000000000', 4, 'Seguir as regras da CVM', FALSE),
    ('ffffffff-0019-0002-0001-000000000000', 'ffffffff-0019-0002-0000-000000000000', 1, 'Comprar logo, antes que suba mais', FALSE),
    ('ffffffff-0019-0002-0002-000000000000', 'ffffffff-0019-0002-0000-000000000000', 2, 'Olhar os números da empresa, como lucro e P/L, antes de decidir', TRUE),
    ('ffffffff-0019-0002-0003-000000000000', 'ffffffff-0019-0002-0000-000000000000', 3, 'Vender tudo o que tem para comprar', FALSE),
    ('ffffffff-0019-0002-0004-000000000000', 'ffffffff-0019-0002-0000-000000000000', 4, 'Pegar um empréstimo para investir nela', FALSE),
    ('ffffffff-0019-0003-0001-000000000000', 'ffffffff-0019-0003-0000-000000000000', 1, 'Os preços sobem muito além do que as empresas valem e depois despencam', TRUE),
    ('ffffffff-0019-0003-0002-000000000000', 'ffffffff-0019-0003-0000-000000000000', 2, 'Os preços ficam estáveis', FALSE),
    ('ffffffff-0019-0003-0003-000000000000', 'ffffffff-0019-0003-0000-000000000000', 3, 'O governo garante os preços', FALSE),
    ('ffffffff-0019-0003-0004-000000000000', 'ffffffff-0019-0003-0000-000000000000', 4, 'Só as empresas pequenas sobem', FALSE);

-- Aula 20: Ancoragem e excesso de confiança (vídeo: BTG Trader)
INSERT INTO articles (id, module_id, title, content, duration_min, display_order, video_id) VALUES
    ('bbbbbbbb-0020-0000-0000-000000000020', 'aaaaaaaa-0007-0000-0000-000000000007', 'Ancoragem e excesso de confiança',
     'Ancoragem é se prender a um número que apareceu primeiro, como o preço de compra de uma ação ou a máxima que ela já atingiu, e julgar tudo a partir dele. Excesso de confiança é acreditar que sabe mais do que sabe: quem acerta algumas vezes tende a achar que vai acertar sempre, concentra a carteira e opera demais. Os dois vieses levam a decisões ruins. Uma defesa simples é anotar o motivo de cada investimento e revisar, de tempos em tempos, se ele ainda vale.',
     4, 3, 'GcOWbj0cLDo');
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
    ('ffffffff-0020-0001-0000-000000000000', 'bbbbbbbb-0020-0000-0000-000000000020', 1,
     'O que é ancoragem?',
     'A âncora pode ser o preço que você pagou, a máxima histórica ou um palpite que ouviu: números que não dizem quanto a empresa vale hoje.'),
    ('ffffffff-0020-0002-0000-000000000000', 'bbbbbbbb-0020-0000-0000-000000000020', 2,
     'Qual é um sinal de excesso de confiança?',
     'Alguns acertos podem ter sido sorte. Concentrar tudo na próxima aposta aumenta muito o risco de uma perda grande.'),
    ('ffffffff-0020-0003-0000-000000000000', 'bbbbbbbb-0020-0000-0000-000000000020', 3,
     'Uma ação já valeu R$ 50 e hoje está em R$ 30. Alguém diz: "está barata, já valeu R$ 50". Qual é o problema desse raciocínio?',
     'A máxima antiga não diz nada sobre o valor de hoje; talvez o lucro tenha caído. Para saber se está barata, olhe indicadores como o P/L e o lucro.');
INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
    ('ffffffff-0020-0001-0001-000000000000', 'ffffffff-0020-0001-0000-000000000000', 1, 'Prender o julgamento a um número que apareceu primeiro, como o preço de compra', TRUE),
    ('ffffffff-0020-0001-0002-000000000000', 'ffffffff-0020-0001-0000-000000000000', 2, 'Investir só em empresas de navegação', FALSE),
    ('ffffffff-0020-0001-0003-000000000000', 'ffffffff-0020-0001-0000-000000000000', 3, 'Travar a taxa de um título', FALSE),
    ('ffffffff-0020-0001-0004-000000000000', 'ffffffff-0020-0001-0000-000000000000', 4, 'Guardar dinheiro em casa', FALSE),
    ('ffffffff-0020-0002-0001-000000000000', 'ffffffff-0020-0002-0000-000000000000', 1, 'Anotar o motivo de cada investimento', FALSE),
    ('ffffffff-0020-0002-0002-000000000000', 'ffffffff-0020-0002-0000-000000000000', 2, 'Diversificar', FALSE),
    ('ffffffff-0020-0002-0003-000000000000', 'ffffffff-0020-0002-0000-000000000000', 3, 'Acertar algumas vezes e concentrar toda a carteira na próxima aposta', TRUE),
    ('ffffffff-0020-0002-0004-000000000000', 'ffffffff-0020-0002-0000-000000000000', 4, 'Comparar a carteira com o CDI', FALSE),
    ('ffffffff-0020-0003-0001-000000000000', 'ffffffff-0020-0003-0000-000000000000', 1, 'Nenhum: ela vai voltar para R$ 50', FALSE),
    ('ffffffff-0020-0003-0002-000000000000', 'ffffffff-0020-0003-0000-000000000000', 2, 'R$ 30 é sempre um preço barato', FALSE),
    ('ffffffff-0020-0003-0003-000000000000', 'ffffffff-0020-0003-0000-000000000000', 3, 'O certo é esperar cair para R$ 10', FALSE),
    ('ffffffff-0020-0003-0004-000000000000', 'ffffffff-0020-0003-0000-000000000000', 4, 'O preço antigo virou âncora: o que importa é quanto a empresa vale hoje', TRUE);

