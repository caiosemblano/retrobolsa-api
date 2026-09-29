-- ============================================================
-- V18: a descrição da rodada 2011–2013 não conta mais o futuro
-- ============================================================
-- A descrição do V13 dizia que a Selic cairia até 7,25% e que "em 2013 os
-- juros voltam a disparar": informação que o jogador, montando a carteira no
-- começo de 2011, não teria. A nova descrição usa só o que se sabia em janeiro
-- de 2011. O que aconteceu depois fica no "o que aconteceu de verdade" (V15),
-- mostrado só no resultado.
--
-- Só troca o texto original: se o admin já tiver editado a descrição, ela fica.
-- ============================================================

UPDATE competitions
SET scenario_description = 'Começo de 2011: depois de crescer 7,5% em 2010, o Brasil está otimista. O novo governo quer juros mais baixos e crédito farto nos bancos públicos, e segura o preço da gasolina e da energia para conter a inflação, que fechou 2010 perto do teto da meta. A Selic está em 10,75%. Enquanto isso, a petroleira de um dos homens mais ricos do mundo promete bilhões de barris no pré-sal, e suas ações estão entre as mais negociadas da bolsa. Em quem você confiaria?'
WHERE scenario_title = 'Nova matriz econômica (2011–2013)'
  AND start_year = 2011
  AND scenario_description LIKE '%em 2013 os juros voltam a disparar%';
