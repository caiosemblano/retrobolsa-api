"""Gera a migration com os indicadores macroeconômicos anuais do Brasil.

Fontes (todas públicas, consultadas na hora de gerar):
  Banco Central, SGS (https://api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados)
    4390  Selic acumulada no mês (%)            -> selic: composta no ano
    432   Meta da Selic definida pelo Copom       -> selic_meta: último valor do ano (existe desde 1999)
    4391  CDI acumulado no mês (%)               -> cdi: composto no ano
    433   IPCA, variação mensal (%)              -> ipca: composto no ano
    7828  Poupança, regra antiga (mensal, aniversário dia 1) -> poupanca até maio/2012
    195   Poupança, regra nova (aniversário dia 1)   -> poupanca a partir de junho/2012
    3696  Dólar PTAX venda, fim de período mensal -> dolar: valor de dezembro
    7326  PIB, variação real anual (%)            -> pib
    7     Ibovespa, fechamento diário             -> ibovespa até 2018
  Yahoo Finance, ^BVSP diário                     -> ibovespa de 2019 em diante
    (o Banco Central parou de publicar o Ibovespa em 2019; o script confere que as duas
     fontes batem no fim de 2015 a 2018 e aborta se não baterem; de 2019 em diante o
     fechamento do Yahoo pode diferir em até ~0,3% do fechamento oficial da B3)

A poupança segue a regra que vale para um depósito novo em cada época: a regra antiga
até maio de 2012 e a nova (70% da Selic + TR quando a Selic está em 8,5% ou menos) depois.

Uso: python tools/macro/gerar_macro.py > src/main/resources/db/migration/V14__create_macro_indicators.sql
"""

import json
import sys
import time
import urllib.request
from datetime import date, datetime, timezone
from decimal import ROUND_HALF_UP, Decimal

PRIMEIRO_ANO = 1995
ULTIMO_ANO = 2025


def baixar(url, tentativas=4):
    """GET com novas tentativas: a API do SGS às vezes devolve corpo vazio sob carga."""
    pedido = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0 (retrobolsa-macro)"})
    for tentativa in range(1, tentativas + 1):
        try:
            with urllib.request.urlopen(pedido, timeout=60) as resposta:
                return json.load(resposta)
        except json.JSONDecodeError:
            if tentativa == tentativas:
                raise
            time.sleep(3 * tentativa)


def sgs(codigo, inicio=date(PRIMEIRO_ANO - 1, 1, 1), fim=date(ULTIMO_ANO, 12, 31)):
    """Série do SGS como lista de (data, valor). Busca em janelas de 10 anos (limite da API)."""
    pontos = []
    ano = inicio.year
    while ano <= fim.year:
        ate = min(ano + 9, fim.year)
        url = (
            f"https://api.bcb.gov.br/dados/serie/bcdata.sgs.{codigo}/dados?formato=json"
            f"&dataInicial=01/01/{ano}&dataFinal=31/12/{ate}"
        )
        try:
            dados = baixar(url)
        except urllib.error.HTTPError as erro:
            if erro.code == 404:  # janela sem dados (série começa depois ou terminou antes)
                dados = []
            else:
                raise
        for item in dados:
            if item["valor"] in (None, ""):  # dia sem cotação publicada
                continue
            pontos.append((datetime.strptime(item["data"], "%d/%m/%Y").date(), Decimal(item["valor"])))
        ano = ate + 1
    return pontos


def composto(pontos_do_ano):
    """Acumula variações mensais em % numa variação anual em %."""
    fator = Decimal(1)
    for valor in pontos_do_ano:
        fator *= 1 + valor / 100
    return (fator - 1) * 100


def por_ano(pontos):
    anos = {}
    for dia, valor in pontos:
        anos.setdefault(dia.year, []).append((dia, valor))
    return anos


def mensal_composto(codigo):
    anos = por_ano(sgs(codigo))
    return {a: composto(v for _, v in sorted(p)) for a, p in anos.items() if len(p) == 12}


def ultimo_do_ano(pontos):
    return {a: sorted(p)[-1][1] for a, p in por_ano(pontos).items()}


def poupanca():
    antiga = dict(sgs(7828))
    nova = {d: v for d, v in sgs(195) if d.day == 1}
    resultado = {}
    for ano in range(PRIMEIRO_ANO, ULTIMO_ANO + 1):
        meses = []
        for mes in range(1, 13):
            dia = date(ano, mes, 1)
            # A regra nova vale para depósitos a partir de 04/05/2012: o primeiro
            # aniversário no dia 1 sob ela é junho de 2012.
            fonte = antiga if dia < date(2012, 6, 1) else nova
            meses.append(fonte[dia])
        resultado[ano] = composto(meses)
    return resultado


def ibovespa():
    """Variação anual do Ibovespa, pelo fechamento do último pregão de cada ano.

    Até 2018 usa o Banco Central (a série 7 termina em setembro de 2019). Em 03/03/1997
    o índice foi dividido por 10, então os fechamentos anteriores são levados à escala nova.
    De 2019 em diante usa o fechamento diário do Yahoo, conferido contra o Banco Central
    no fim de 2015 a 2018: se alguma diferença passar de 0,5%, o script aborta. (Os dados
    do Yahoo antes de 2015 têm erros de até 20% e não são usados.)
    """
    bc = ultimo_do_ano(sgs(7, inicio=date(1994, 1, 1), fim=date(2018, 12, 31)))
    for ano in (1994, 1995, 1996):
        bc[ano] = bc[ano] / 10

    # Intervalo explícito: com range=max o Yahoo devolve barras mensais, e não diárias.
    inicio = int(datetime(2015, 1, 1, tzinfo=timezone.utc).timestamp())
    fim = int(datetime(ULTIMO_ANO + 1, 1, 10, tzinfo=timezone.utc).timestamp())
    grafico = baixar(
        f"https://query1.finance.yahoo.com/v8/finance/chart/%5EBVSP?interval=1d&period1={inicio}&period2={fim}"
    )["chart"]["result"][0]
    yahoo = {}
    for instante, fechamento in zip(grafico["timestamp"], grafico["indicators"]["quote"][0]["close"]):
        if fechamento is None:
            continue
        # O pregão é em horário de Brasília; o último do ano sobrescreve os anteriores.
        dia = datetime.fromtimestamp(instante + grafico["meta"]["gmtoffset"], timezone.utc).date()
        yahoo[dia.year] = Decimal(str(fechamento))

    for ano in range(2015, 2019):
        if abs(yahoo[ano] / bc[ano] - 1) > Decimal("0.005"):
            sys.exit(f"Ibovespa {ano}: Yahoo {yahoo[ano]} x Banco Central {bc[ano]}")

    fechamentos = {**bc, **{a: v for a, v in yahoo.items() if a >= 2019}}
    return {a: (fechamentos[a] / fechamentos[a - 1] - 1) * 100 for a in fechamentos if a - 1 in fechamentos}


def fmt(valor, casas=2):
    if valor is None:
        return "NULL"
    return str(Decimal(valor).quantize(Decimal(1).scaleb(-casas), rounding=ROUND_HALF_UP))


def main():
    # O Flyway lê a migration em UTF-8; no Windows a saída padrão seria cp1252.
    sys.stdout.reconfigure(encoding="utf-8", newline="\n")
    selic = mensal_composto(4390)
    cdi = mensal_composto(4391)
    ipca = mensal_composto(433)
    meta = ultimo_do_ano(sgs(432, inicio=date(1999, 1, 1)))
    dolar = {d.year: v for d, v in sgs(3696) if d.month == 12}
    pib = {d.year: v for d, v in sgs(7326)}
    poup = poupanca()
    ibov = ibovespa()

    linhas = []
    for ano in range(PRIMEIRO_ANO, ULTIMO_ANO + 1):
        valores = [selic[ano], meta.get(ano), cdi[ano], ipca[ano], poup[ano], ibov[ano], dolar[ano], pib[ano]]
        casas = [2, 2, 2, 2, 2, 2, 4, 2]
        linhas.append(f"    ({ano}, " + ", ".join(fmt(v, c) for v, c in zip(valores, casas)) + ")")

    print(f"""-- ============================================================
-- V14: indicadores macroeconômicos anuais do Brasil ({PRIMEIRO_ANO}–{ULTIMO_ANO})
-- ============================================================
-- Gerado por tools/macro/gerar_macro.py em {date.today().isoformat()}; não editar à mão.
-- Usados para mostrar o cenário econômico de cada rodada e, no resultado,
-- comparar a carteira com CDI, poupança, inflação e Ibovespa.
--
-- Todas as taxas em % no ano, salvo indicação:
--   selic       Selic acumulada no ano            (BCB SGS 4390, composta)
--   selic_meta  meta da Selic no fim do ano, % a.a. (BCB SGS 432; existe desde 1999)
--   cdi         CDI acumulado no ano              (BCB SGS 4391, composto)
--   ipca        inflação oficial no ano           (BCB SGS 433, composta)
--   poupanca    rendimento de um depósito novo    (BCB SGS 7828 até mai/2012, 195 depois)
--   ibovespa    variação do índice no ano         (BCB SGS 7 até 2018; Yahoo Finance ^BVSP depois,
--               conferido com o BCB em 2015–2018; de 2019 em diante pode diferir ~0,3% do oficial)
--   dolar       R$ por US$ no fim do ano          (BCB SGS 3696, PTAX venda de dezembro)
--   pib         crescimento real do PIB           (BCB SGS 7326)
-- ============================================================

CREATE TABLE macro_indicators (
    year        INT PRIMARY KEY,
    selic       NUMERIC(8, 2) NOT NULL,
    selic_meta  NUMERIC(6, 2),
    cdi         NUMERIC(8, 2) NOT NULL,
    ipca        NUMERIC(8, 2) NOT NULL,
    poupanca    NUMERIC(8, 2) NOT NULL,
    ibovespa    NUMERIC(8, 2) NOT NULL,
    dolar       NUMERIC(8, 4) NOT NULL,
    pib         NUMERIC(6, 2) NOT NULL
);

INSERT INTO macro_indicators (year, selic, selic_meta, cdi, ipca, poupanca, ibovespa, dolar, pib) VALUES
{",\n".join(linhas)};""")


if __name__ == "__main__":
    main()
