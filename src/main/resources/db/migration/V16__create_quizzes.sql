-- ============================================================
-- V16: quizzes das aulas
-- ============================================================
-- Cada aula pode ter perguntas de múltipla escolha. Acertar 2 de 3 (ou mais)
-- conclui a aula; cada pergunta traz a explicação da resposta certa, mostrada
-- logo depois de responder.
--
-- user_quiz_answers guarda cada resposta de cada tentativa: é o que permite,
-- mais adiante, mostrar ao professor as perguntas que a turma mais erra.
--
-- IDs legíveis (família ffffffff-): pergunta ffffffff-00AA-000P-0000-…,
-- alternativa ffffffff-00AA-000P-000O-…, onde AA é o número da aula
-- (bbbbbbbb-00AA), P a pergunta e O a alternativa.
-- ============================================================

CREATE TABLE quiz_questions (
    id            UUID PRIMARY KEY,
    article_id    UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    display_order INT NOT NULL,
    prompt        VARCHAR(400) NOT NULL,
    explanation   VARCHAR(600) NOT NULL,
    UNIQUE (article_id, display_order)
);

CREATE TABLE quiz_options (
    id            UUID PRIMARY KEY,
    question_id   UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    display_order INT NOT NULL,
    text          VARCHAR(300) NOT NULL,
    correct       BOOLEAN NOT NULL DEFAULT FALSE,
    UNIQUE (question_id, display_order)
);
-- Exatamente uma alternativa certa por pergunta (a "no máximo uma" o banco garante;
-- a "pelo menos uma", o teste da migration).
CREATE UNIQUE INDEX uq_quiz_options_one_correct ON quiz_options(question_id) WHERE correct;

CREATE TABLE user_quiz_attempts (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    article_id  UUID NOT NULL REFERENCES articles(id) ON DELETE CASCADE,
    score       INT NOT NULL,
    total       INT NOT NULL,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_user_quiz_attempts_user_article ON user_quiz_attempts(user_id, article_id);

CREATE TABLE user_quiz_answers (
    attempt_id  UUID NOT NULL REFERENCES user_quiz_attempts(id) ON DELETE CASCADE,
    question_id UUID NOT NULL REFERENCES quiz_questions(id) ON DELETE CASCADE,
    option_id   UUID NOT NULL REFERENCES quiz_options(id) ON DELETE CASCADE,
    correct     BOOLEAN NOT NULL,
    PRIMARY KEY (attempt_id, question_id)
);
CREATE INDEX idx_user_quiz_answers_question ON user_quiz_answers(question_id);

-- ------------------------------------------------------------
-- Aula 1: O que é rentabilidade?
-- ------------------------------------------------------------
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
('ffffffff-0001-0001-0000-000000000000', 'bbbbbbbb-0001-0000-0000-000000000001', 1,
 'Você investiu R$ 2.000 e, no fim do período, tinha R$ 2.300. Qual foi a rentabilidade?',
 'Rentabilidade é o ganho em relação ao valor aplicado: 300 ÷ 2.000 = 0,15, ou seja, 15%. R$ 300 é o ganho em reais, não a rentabilidade.'),
('ffffffff-0001-0002-0000-000000000000', 'bbbbbbbb-0001-0000-0000-000000000001', 2,
 'Duas carteiras ganharam R$ 500. Uma começou com R$ 1.000 e a outra com R$ 10.000. Qual rendeu mais?',
 'O mesmo ganho em reais pode ser uma rentabilidade muito diferente: 500 ÷ 1.000 = 50%, e 500 ÷ 10.000 = 5%. Por isso investimentos se comparam em porcentagem.'),
('ffffffff-0001-0003-0000-000000000000', 'bbbbbbbb-0001-0000-0000-000000000001', 3,
 'Uma carteira de R$ 100.000 terminou a rodada valendo R$ 90.000, sem que nada fosse vendido. Qual foi a rentabilidade?',
 'Perda também é rentabilidade, só que negativa: (90.000 − 100.000) ÷ 100.000 = −10%. Não vender não muda o fato de a carteira valer menos.');

INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
('ffffffff-0001-0001-0001-000000000000', 'ffffffff-0001-0001-0000-000000000000', 1, 'R$ 300', FALSE),
('ffffffff-0001-0001-0002-000000000000', 'ffffffff-0001-0001-0000-000000000000', 2, '15%', TRUE),
('ffffffff-0001-0001-0003-000000000000', 'ffffffff-0001-0001-0000-000000000000', 3, '13%', FALSE),
('ffffffff-0001-0001-0004-000000000000', 'ffffffff-0001-0001-0000-000000000000', 4, '30%', FALSE),
('ffffffff-0001-0002-0001-000000000000', 'ffffffff-0001-0002-0000-000000000000', 1, 'As duas, porque ganharam o mesmo valor', FALSE),
('ffffffff-0001-0002-0002-000000000000', 'ffffffff-0001-0002-0000-000000000000', 2, 'A de R$ 10.000, porque tinha mais dinheiro', FALSE),
('ffffffff-0001-0002-0003-000000000000', 'ffffffff-0001-0002-0000-000000000000', 3, 'A de R$ 1.000: 50% contra 5%', TRUE),
('ffffffff-0001-0002-0004-000000000000', 'ffffffff-0001-0002-0000-000000000000', 4, 'Não dá para saber', FALSE),
('ffffffff-0001-0003-0001-000000000000', 'ffffffff-0001-0003-0000-000000000000', 1, '−10%', TRUE),
('ffffffff-0001-0003-0002-000000000000', 'ffffffff-0001-0003-0000-000000000000', 2, '10%', FALSE),
('ffffffff-0001-0003-0003-000000000000', 'ffffffff-0001-0003-0000-000000000000', 3, '0%, porque nada foi vendido', FALSE),
('ffffffff-0001-0003-0004-000000000000', 'ffffffff-0001-0003-0000-000000000000', 4, '−R$ 90.000', FALSE);

-- ------------------------------------------------------------
-- Aula 2: Juros simples vs. compostos
-- ------------------------------------------------------------
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
('ffffffff-0002-0001-0000-000000000000', 'bbbbbbbb-0002-0000-0000-000000000002', 1,
 'R$ 1.000 aplicados a 10% ao ano, com juros compostos, viram quanto em 2 anos?',
 'No 1º ano, R$ 1.000 viram R$ 1.100. No 2º, os 10% incidem sobre R$ 1.100 e rendem R$ 110: total de R$ 1.210. Com juros simples seriam R$ 1.200.'),
('ffffffff-0002-0002-0000-000000000000', 'bbbbbbbb-0002-0000-0000-000000000002', 2,
 'Qual é a diferença entre juros simples e compostos?',
 'Nos juros simples, o rendimento é sempre calculado sobre o valor inicial; nos compostos, sobre o valor acumulado. Quanto mais longo o prazo, maior a diferença.'),
('ffffffff-0002-0003-0000-000000000000', 'bbbbbbbb-0002-0000-0000-000000000002', 3,
 'Uma carteira ganhou 50% num ano e perdeu 50% no ano seguinte. Como ela terminou?',
 'Os retornos se multiplicam: 1,5 × 0,5 = 0,75. R$ 100 viram R$ 150 e depois R$ 75. Depois de perder 50%, é preciso ganhar 100% para voltar ao ponto de partida.');

INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
('ffffffff-0002-0001-0001-000000000000', 'ffffffff-0002-0001-0000-000000000000', 1, 'R$ 1.200', FALSE),
('ffffffff-0002-0001-0002-000000000000', 'ffffffff-0002-0001-0000-000000000000', 2, 'R$ 1.210', TRUE),
('ffffffff-0002-0001-0003-000000000000', 'ffffffff-0002-0001-0000-000000000000', 3, 'R$ 1.100', FALSE),
('ffffffff-0002-0001-0004-000000000000', 'ffffffff-0002-0001-0000-000000000000', 4, 'R$ 1.020', FALSE),
('ffffffff-0002-0002-0001-000000000000', 'ffffffff-0002-0002-0000-000000000000', 1, 'Nos compostos, os juros de cada período passam a render juros', TRUE),
('ffffffff-0002-0002-0002-000000000000', 'ffffffff-0002-0002-0000-000000000000', 2, 'Os juros simples sempre rendem mais', FALSE),
('ffffffff-0002-0002-0003-000000000000', 'ffffffff-0002-0002-0000-000000000000', 3, 'Juros compostos só existem em empréstimos', FALSE),
('ffffffff-0002-0002-0004-000000000000', 'ffffffff-0002-0002-0000-000000000000', 4, 'Em prazos longos não há diferença', FALSE),
('ffffffff-0002-0003-0001-000000000000', 'ffffffff-0002-0003-0000-000000000000', 1, 'Igual ao começo', FALSE),
('ffffffff-0002-0003-0002-000000000000', 'ffffffff-0002-0003-0000-000000000000', 2, 'Com 50% a menos', FALSE),
('ffffffff-0002-0003-0003-000000000000', 'ffffffff-0002-0003-0000-000000000000', 3, 'Com 25% a mais', FALSE),
('ffffffff-0002-0003-0004-000000000000', 'ffffffff-0002-0003-0000-000000000000', 4, 'Com 25% a menos', TRUE);

-- ------------------------------------------------------------
-- Aula 3: Como calcular o retorno anualizado
-- ------------------------------------------------------------
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
('ffffffff-0003-0001-0000-000000000000', 'bbbbbbbb-0003-0000-0000-000000000003', 1,
 'Para que serve o retorno anualizado?',
 'Ele transforma o ganho de vários anos numa taxa por ano. Assim, 21% em 2 anos e 30% em 3 anos podem ser comparados na mesma régua.'),
('ffffffff-0003-0002-0000-000000000000', 'bbbbbbbb-0003-0000-0000-000000000003', 2,
 'Uma carteira rendeu 21% em 2 anos. Qual foi o retorno anualizado?',
 '1,21 elevado a 1/2 dá 1,10: 10% ao ano. Dividir 21% por 2 dá 10,5%, mas ignora que os juros de um ano rendem no ano seguinte.'),
('ffffffff-0003-0003-0000-000000000000', 'bbbbbbbb-0003-0000-0000-000000000003', 3,
 'O investimento A rendeu 44% em 4 anos, e o B rendeu 21% em 2 anos. Qual rendeu mais por ano?',
 '1,44 elevado a 1/4 dá cerca de 1,095, ou 9,5% ao ano; 1,21 elevado a 1/2 dá 1,10, ou 10% ao ano. Um total maior pode esconder um ritmo menor.');

INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
('ffffffff-0003-0001-0001-000000000000', 'ffffffff-0003-0001-0000-000000000000', 1, 'Para saber quanto imposto pagar', FALSE),
('ffffffff-0003-0001-0002-000000000000', 'ffffffff-0003-0001-0000-000000000000', 2, 'Para prever quanto vai render no próximo ano', FALSE),
('ffffffff-0003-0001-0003-000000000000', 'ffffffff-0003-0001-0000-000000000000', 3, 'Para comparar investimentos de prazos diferentes', TRUE),
('ffffffff-0003-0001-0004-000000000000', 'ffffffff-0003-0001-0000-000000000000', 4, 'Para somar o retorno de todos os anos', FALSE),
('ffffffff-0003-0002-0001-000000000000', 'ffffffff-0003-0002-0000-000000000000', 1, '10,5% ao ano', FALSE),
('ffffffff-0003-0002-0002-000000000000', 'ffffffff-0003-0002-0000-000000000000', 2, '10% ao ano', TRUE),
('ffffffff-0003-0002-0003-000000000000', 'ffffffff-0003-0002-0000-000000000000', 3, '21% ao ano', FALSE),
('ffffffff-0003-0002-0004-000000000000', 'ffffffff-0003-0002-0000-000000000000', 4, '11% ao ano', FALSE),
('ffffffff-0003-0003-0001-000000000000', 'ffffffff-0003-0003-0000-000000000000', 1, 'A, porque 44% é mais que 21%', FALSE),
('ffffffff-0003-0003-0002-000000000000', 'ffffffff-0003-0003-0000-000000000000', 2, 'Os dois renderam o mesmo por ano', FALSE),
('ffffffff-0003-0003-0003-000000000000', 'ffffffff-0003-0003-0000-000000000000', 3, 'A: cerca de 11% ao ano', FALSE),
('ffffffff-0003-0003-0004-000000000000', 'ffffffff-0003-0003-0000-000000000000', 4, 'B: cerca de 10% ao ano, contra 9,5% de A', TRUE);

-- ------------------------------------------------------------
-- Aula 4: O que é P/L?
-- ------------------------------------------------------------
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
('ffffffff-0004-0001-0000-000000000000', 'bbbbbbbb-0004-0000-0000-000000000004', 1,
 'Uma ação custa R$ 40, e a empresa lucra R$ 5 por ação ao ano. Qual é o P/L?',
 'P/L = preço ÷ lucro por ação = 40 ÷ 5 = 8. Mantido o lucro, a empresa levaria 8 anos para gerar o valor que você pagou pela ação.'),
('ffffffff-0004-0002-0000-000000000000', 'bbbbbbbb-0004-0000-0000-000000000004', 2,
 'Um P/L bem mais baixo que o de empresas parecidas pode indicar:',
 'P/L baixo é um convite para investigar, não uma garantia: às vezes a ação está barata, às vezes o mercado espera que o lucro caia.'),
('ffffffff-0004-0003-0000-000000000000', 'bbbbbbbb-0004-0000-0000-000000000004', 3,
 'Como fica o P/L de uma empresa que teve prejuízo?',
 'Com lucro negativo, a divisão dá um P/L negativo, que não serve para comparar empresas. Por isso vale olhar também se a empresa teve lucro no ano.');

INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
('ffffffff-0004-0001-0001-000000000000', 'ffffffff-0004-0001-0000-000000000000', 1, '8', TRUE),
('ffffffff-0004-0001-0002-000000000000', 'ffffffff-0004-0001-0000-000000000000', 2, '5', FALSE),
('ffffffff-0004-0001-0003-000000000000', 'ffffffff-0004-0001-0000-000000000000', 3, '40', FALSE),
('ffffffff-0004-0001-0004-000000000000', 'ffffffff-0004-0001-0000-000000000000', 4, '0,125', FALSE),
('ffffffff-0004-0002-0001-000000000000', 'ffffffff-0004-0002-0000-000000000000', 1, 'Que a ação certamente vai subir', FALSE),
('ffffffff-0004-0002-0002-000000000000', 'ffffffff-0004-0002-0000-000000000000', 2, 'Que a ação está barata ou que o mercado desconfia da empresa', TRUE),
('ffffffff-0004-0002-0003-000000000000', 'ffffffff-0004-0002-0000-000000000000', 3, 'Que a empresa não dá lucro', FALSE),
('ffffffff-0004-0002-0004-000000000000', 'ffffffff-0004-0002-0000-000000000000', 4, 'Que a empresa paga muitos dividendos', FALSE),
('ffffffff-0004-0003-0001-000000000000', 'ffffffff-0004-0003-0000-000000000000', 1, 'Fica muito alto', FALSE),
('ffffffff-0004-0003-0002-000000000000', 'ffffffff-0004-0003-0000-000000000000', 2, 'Fica igual ao preço da ação', FALSE),
('ffffffff-0004-0003-0003-000000000000', 'ffffffff-0004-0003-0000-000000000000', 3, 'Fica negativo, e o indicador perde o sentido', TRUE),
('ffffffff-0004-0003-0004-000000000000', 'ffffffff-0004-0003-0000-000000000000', 4, 'Fica igual a zero', FALSE);

-- ------------------------------------------------------------
-- Aula 5: O que é ROE?
-- ------------------------------------------------------------
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
('ffffffff-0005-0001-0000-000000000000', 'bbbbbbbb-0005-0000-0000-000000000005', 1,
 'Uma empresa lucrou R$ 30 milhões no ano e tem patrimônio líquido de R$ 150 milhões. Qual é o ROE?',
 'ROE = lucro ÷ patrimônio = 30 ÷ 150 = 0,20, ou 20%: cada R$ 100 dos sócios gerou R$ 20 de lucro no ano.'),
('ffffffff-0005-0002-0000-000000000000', 'bbbbbbbb-0005-0000-0000-000000000005', 2,
 'O que o ROE mostra?',
 'O ROE mede a eficiência da empresa em transformar o capital dos sócios em lucro. Não diz nada, sozinho, sobre o preço da ação ou sobre dividendos.'),
('ffffffff-0005-0003-0000-000000000000', 'bbbbbbbb-0005-0000-0000-000000000005', 3,
 'Por que um ROE muito alto merece uma segunda olhada?',
 'Com muita dívida e pouco patrimônio, a divisão lucro ÷ patrimônio fica grande mesmo sem eficiência. O ROE é bom sinal quando é alto e consistente sem depender de endividamento.');

INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
('ffffffff-0005-0001-0001-000000000000', 'ffffffff-0005-0001-0000-000000000000', 1, '5%', FALSE),
('ffffffff-0005-0001-0002-000000000000', 'ffffffff-0005-0001-0000-000000000000', 2, '30%', FALSE),
('ffffffff-0005-0001-0003-000000000000', 'ffffffff-0005-0001-0000-000000000000', 3, '20%', TRUE),
('ffffffff-0005-0001-0004-000000000000', 'ffffffff-0005-0001-0000-000000000000', 4, '50%', FALSE),
('ffffffff-0005-0002-0001-000000000000', 'ffffffff-0005-0002-0000-000000000000', 1, 'Quanto a ação subiu no ano', FALSE),
('ffffffff-0005-0002-0002-000000000000', 'ffffffff-0005-0002-0000-000000000000', 2, 'Quanto lucro a empresa gera com o dinheiro dos sócios', TRUE),
('ffffffff-0005-0002-0003-000000000000', 'ffffffff-0005-0002-0000-000000000000', 3, 'Quanto a empresa paga de dividendos', FALSE),
('ffffffff-0005-0002-0004-000000000000', 'ffffffff-0005-0002-0000-000000000000', 4, 'Quanto a empresa deve aos bancos', FALSE),
('ffffffff-0005-0003-0001-000000000000', 'ffffffff-0005-0003-0000-000000000000', 1, 'Porque pode vir de dívida alta, e não de eficiência', TRUE),
('ffffffff-0005-0003-0002-000000000000', 'ffffffff-0005-0003-0000-000000000000', 2, 'Porque ROE alto sempre indica fraude', FALSE),
('ffffffff-0005-0003-0003-000000000000', 'ffffffff-0005-0003-0000-000000000000', 3, 'Porque significa que a ação está cara', FALSE),
('ffffffff-0005-0003-0004-000000000000', 'ffffffff-0005-0003-0000-000000000000', 4, 'Porque empresas com ROE alto não pagam dividendos', FALSE);

-- ------------------------------------------------------------
-- Aula 6: Dividend Yield na prática
-- ------------------------------------------------------------
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
('ffffffff-0006-0001-0000-000000000000', 'bbbbbbbb-0006-0000-0000-000000000006', 1,
 'Uma ação de R$ 25 pagou R$ 2 de dividendos nos últimos 12 meses. Qual é o Dividend Yield?',
 'DY = dividendos ÷ preço = 2 ÷ 25 = 0,08, ou 8%.'),
('ffffffff-0006-0002-0000-000000000000', 'bbbbbbbb-0006-0000-0000-000000000006', 2,
 'O preço de uma ação caiu pela metade, e os dividendos pagos no último ano foram os mesmos. O que acontece com o DY?',
 'O DY divide pelo preço atual: com o preço pela metade, o DY dobra. Por isso um DY alto pode ser efeito de uma queda forte, e os dividendos futuros podem não se repetir.'),
('ffffffff-0006-0003-0000-000000000000', 'bbbbbbbb-0006-0000-0000-000000000006', 3,
 'Quem quer receber renda em dinheiro das ações, sem precisar vendê-las, costuma olhar principalmente:',
 'Dividendos são a parte do lucro paga aos acionistas em dinheiro, e o DY mostra quanto isso representa em relação ao preço da ação.');

INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
('ffffffff-0006-0001-0001-000000000000', 'ffffffff-0006-0001-0000-000000000000', 1, '2%', FALSE),
('ffffffff-0006-0001-0002-000000000000', 'ffffffff-0006-0001-0000-000000000000', 2, '12,5%', FALSE),
('ffffffff-0006-0001-0003-000000000000', 'ffffffff-0006-0001-0000-000000000000', 3, '25%', FALSE),
('ffffffff-0006-0001-0004-000000000000', 'ffffffff-0006-0001-0000-000000000000', 4, '8%', TRUE),
('ffffffff-0006-0002-0001-000000000000', 'ffffffff-0006-0002-0000-000000000000', 1, 'Cai pela metade', FALSE),
('ffffffff-0006-0002-0002-000000000000', 'ffffffff-0006-0002-0000-000000000000', 2, 'Dobra', TRUE),
('ffffffff-0006-0002-0003-000000000000', 'ffffffff-0006-0002-0000-000000000000', 3, 'Não muda', FALSE),
('ffffffff-0006-0002-0004-000000000000', 'ffffffff-0006-0002-0000-000000000000', 4, 'Vai a zero', FALSE),
('ffffffff-0006-0003-0001-000000000000', 'ffffffff-0006-0003-0000-000000000000', 1, 'O Dividend Yield', TRUE),
('ffffffff-0006-0003-0002-000000000000', 'ffffffff-0006-0003-0000-000000000000', 2, 'O P/L', FALSE),
('ffffffff-0006-0003-0003-000000000000', 'ffffffff-0006-0003-0000-000000000000', 3, 'A margem EBITDA', FALSE),
('ffffffff-0006-0003-0004-000000000000', 'ffffffff-0006-0003-0000-000000000000', 4, 'O crescimento da receita', FALSE);

-- ------------------------------------------------------------
-- Aula 7: O que é a Taxa Selic?
-- ------------------------------------------------------------
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
('ffffffff-0007-0001-0000-000000000000', 'bbbbbbbb-0007-0000-0000-000000000007', 1,
 'Quem define a meta da taxa Selic?',
 'O Copom, o Comitê de Política Monetária do Banco Central, se reúne a cada 45 dias e define a meta da Selic.'),
('ffffffff-0007-0002-0000-000000000000', 'bbbbbbbb-0007-0000-0000-000000000007', 2,
 'Por que o Banco Central sobe a Selic quando a inflação está alta?',
 'Com juros mais altos, pegar empréstimo fica mais caro e guardar dinheiro fica mais atraente. As pessoas e as empresas gastam menos, e os preços sobem mais devagar.'),
('ffffffff-0007-0003-0000-000000000000', 'bbbbbbbb-0007-0000-0000-000000000007', 3,
 'Se a Selic sobe, o que acontece com o rendimento de um título atrelado à Selic?',
 'O Tesouro Selic rende a taxa de cada dia: Selic mais alta, rendimento maior. Já os títulos prefixados e as ações costumam sofrer quando os juros sobem.');

INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
('ffffffff-0007-0001-0001-000000000000', 'ffffffff-0007-0001-0000-000000000000', 1, 'O Congresso Nacional', FALSE),
('ffffffff-0007-0001-0002-000000000000', 'ffffffff-0007-0001-0000-000000000000', 2, 'O Copom, do Banco Central', TRUE),
('ffffffff-0007-0001-0003-000000000000', 'ffffffff-0007-0001-0000-000000000000', 3, 'A bolsa de valores', FALSE),
('ffffffff-0007-0001-0004-000000000000', 'ffffffff-0007-0001-0000-000000000000', 4, 'Os grandes bancos', FALSE),
('ffffffff-0007-0002-0001-000000000000', 'ffffffff-0007-0002-0000-000000000000', 1, 'Para que os bancos lucrem mais', FALSE),
('ffffffff-0007-0002-0002-000000000000', 'ffffffff-0007-0002-0000-000000000000', 2, 'Para fazer a bolsa subir', FALSE),
('ffffffff-0007-0002-0003-000000000000', 'ffffffff-0007-0002-0000-000000000000', 3, 'Porque juros altos encarecem o crédito, esfriam o consumo e seguram os preços', TRUE),
('ffffffff-0007-0002-0004-000000000000', 'ffffffff-0007-0002-0000-000000000000', 4, 'Porque a Selic precisa ser igual à inflação', FALSE),
('ffffffff-0007-0003-0001-000000000000', 'ffffffff-0007-0003-0000-000000000000', 1, 'Não muda', FALSE),
('ffffffff-0007-0003-0002-000000000000', 'ffffffff-0007-0003-0000-000000000000', 2, 'Diminui', FALSE),
('ffffffff-0007-0003-0003-000000000000', 'ffffffff-0007-0003-0000-000000000000', 3, 'O título perde todo o valor', FALSE),
('ffffffff-0007-0003-0004-000000000000', 'ffffffff-0007-0003-0000-000000000000', 4, 'Aumenta', TRUE);

-- ------------------------------------------------------------
-- Aula 8: IPCA: como a inflação corrói seus ganhos
-- ------------------------------------------------------------
INSERT INTO quiz_questions (id, article_id, display_order, prompt, explanation) VALUES
('ffffffff-0008-0001-0000-000000000000', 'bbbbbbbb-0008-0000-0000-000000000008', 1,
 'O que o IPCA mede?',
 'O IPCA é o índice oficial de inflação do Brasil, calculado pelo IBGE a partir dos preços de uma cesta de produtos e serviços consumidos pelas famílias.'),
('ffffffff-0008-0002-0000-000000000000', 'bbbbbbbb-0008-0000-0000-000000000008', 2,
 'Sua carteira rendeu 8% num ano em que a inflação foi de 6%. Quanto seu poder de compra aumentou, aproximadamente?',
 'Ganho real = 1,08 ÷ 1,06 − 1 ≈ 1,9%. É quanto a mais você consegue comprar com o dinheiro; subtrair (8% − 6% = 2%) dá uma aproximação.'),
('ffffffff-0008-0003-0000-000000000000', 'bbbbbbbb-0008-0000-0000-000000000008', 3,
 'Um investimento rendeu 5% num ano de inflação de 10%. O que aconteceu?',
 'Os reais aumentaram, mas os preços subiram mais: 1,05 ÷ 1,10 − 1 ≈ −4,5%. No fim do ano, o dinheiro compra menos do que no começo.');

INSERT INTO quiz_options (id, question_id, display_order, text, correct) VALUES
('ffffffff-0008-0001-0001-000000000000', 'ffffffff-0008-0001-0000-000000000000', 1, 'A variação dos preços de produtos e serviços consumidos pelas famílias', TRUE),
('ffffffff-0008-0001-0002-000000000000', 'ffffffff-0008-0001-0000-000000000000', 2, 'O crescimento da economia', FALSE),
('ffffffff-0008-0001-0003-000000000000', 'ffffffff-0008-0001-0000-000000000000', 3, 'O rendimento da poupança', FALSE),
('ffffffff-0008-0001-0004-000000000000', 'ffffffff-0008-0001-0000-000000000000', 4, 'A cotação do dólar', FALSE),
('ffffffff-0008-0002-0001-000000000000', 'ffffffff-0008-0002-0000-000000000000', 1, '14%', FALSE),
('ffffffff-0008-0002-0002-000000000000', 'ffffffff-0008-0002-0000-000000000000', 2, '8%', FALSE),
('ffffffff-0008-0002-0003-000000000000', 'ffffffff-0008-0002-0000-000000000000', 3, 'Cerca de 1,9%', TRUE),
('ffffffff-0008-0002-0004-000000000000', 'ffffffff-0008-0002-0000-000000000000', 4, '6%', FALSE),
('ffffffff-0008-0003-0001-000000000000', 'ffffffff-0008-0003-0000-000000000000', 1, 'Nada, porque o dinheiro não diminuiu', FALSE),
('ffffffff-0008-0003-0002-000000000000', 'ffffffff-0008-0003-0000-000000000000', 2, 'O poder de compra caiu', TRUE),
('ffffffff-0008-0003-0003-000000000000', 'ffffffff-0008-0003-0000-000000000000', 3, 'O poder de compra subiu 5%', FALSE),
('ffffffff-0008-0003-0004-000000000000', 'ffffffff-0008-0003-0000-000000000000', 4, 'O investimento rendeu 15%', FALSE);
