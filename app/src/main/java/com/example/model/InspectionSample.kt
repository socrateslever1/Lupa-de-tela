package com.example.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.Map
import androidx.compose.ui.graphics.vector.ImageVector

data class InspectionSample(
    val id: String,
    val title: String,
    val category: String,
    val icon: ImageVector,
    val hint: String,
    val sections: List<SampleSection>
)

data class SampleSection(
    val header: String,
    val text: String,
    val isMicroText: Boolean = false
)

object InspectionSamples {
    val samples = listOf(
        InspectionSample(
            id = "embalagem",
            title = "Rótulo de Alimento & Farmácia",
            category = "Embalagens e Medicamentos",
            icon = Icons.Default.LocalPharmacy,
            hint = "Letras miúdas em 4pt: Tabela nutricional e advertência de alergênicos",
            sections = listOf(
                SampleSection(
                    header = "TABELA NUTRICIONAL (Porção 30g)",
                    text = "Valor Energético: 124 kcal = 520 kJ (6% VD*)\nCarboidratos: 18g (6% VD*)\nAçúcares Totais: 3.2g\nAçúcares Adicionados: 1.1g\nProteínas: 2.8g (4% VD*)\nGorduras Totais: 4.5g (8% VD*)\nGorduras Saturadas: 1.2g (5% VD*)\nGorduras Trans: 0.0g (0% VD*)\nFibra Alimentar: 2.1g (8% VD*)\nSódio: 95mg (4% VD*)"
                ),
                SampleSection(
                    header = "LISTA DE INGREDIENTES E CONSERVANTES",
                    text = "Farinha de trigo integral fortificada com ferro e ácido fólico, flocos de aveia laminada, sementes de chia, óleo vegetal de girassol alto oleico, extrato de malte, fermentos químicos (bicarbonato de sódio INS 500ii e pirofosfato dissódico INS 450i), emulsificante lecitina de soja (INS 322), aroma idêntico ao natural de baunilha, antioxidante alfa-tocoferol (INS 307a).",
                    isMicroText = true
                ),
                SampleSection(
                    header = "ADVERTÊNCIA DE ALERGÊNICOS (ANVISA RDC 727)",
                    text = "ALÉRGICOS: CONTÉM DERIVADOS DE TRIGO, SOJA E AVEIA. PODE CONTER CEVADA, CENTEIO, CASTANHA-DE-CAJU, CASTANHA-DO-PARÁ, MACADÂMIAS E LEITE. CONTÉM GLÚTEN. Conservar em local seco, fresco e arejado.",
                    isMicroText = true
                ),
                SampleSection(
                    header = "RASTREABILIDADE E LOTE",
                    text = "FAB: 14/09/2026 08:32 LOTE: BR-99420-EXP VENC: 14/09/2027 SAC: 0800 772 1090",
                    isMicroText = true
                )
            )
        ),
        InspectionSample(
            id = "manual",
            title = "Manual de Instruções e Diagrama",
            category = "Eletrônicos & Eletrodomésticos",
            icon = Icons.Default.Description,
            hint = "Especificações elétricas microscópicas e advertências de segurança",
            sections = listOf(
                SampleSection(
                    header = "ESPECIFICAÇÕES TÉCNICAS (INPUT/OUTPUT)",
                    text = "Entrada: AC 100-240V ~ 50/60Hz 1.8A Max\nSaída USB-C1 (PD 3.1): 5.0V=3.0A (15W), 9.0V=3.0A (27W), 15.0V=3.0A (45W), 20.0V=5.0A (100W Max), PPS 3.3-21.0V=5.0A\nSaída USB-C2/C3: 5.0V=3.0A, 9.0V=3.0A, 20.0V=3.25A (65W Max)\nEficiência energética: Nível VI (DoE/CoC Tier 2) - Consumo em espera < 0.075W\nHomologação Anatel: 04512-24-11890"
                ),
                SampleSection(
                    header = "CUIDADOS E ADVERTÊNCIA DE CIRCUITO",
                    text = "ATENÇÃO: Não exponha este módulo a condensação ou solventes clorados. Abertura do invólucro anula a garantia e acarreta risco de descarga eletrostática (ESD Class 1B). Utilize apenas cabos com chip E-Marker certificado para correntes superiores a 3A.",
                    isMicroText = true
                ),
                SampleSection(
                    header = "CÓDIGOS DE FALHA NO LED INDICADOR",
                    text = "1 Pisca azul: Inicialização OK | 2 Piscadas vermelhas: Sobretensão de entrada (>265V) | Pisca intermitente âmbar: Corte térmico ativo (>78°C). Aguarde 10 minutos para restauração automática.",
                    isMicroText = true
                )
            )
        ),
        InspectionSample(
            id = "mapa",
            title = "Mapa de Transporte & Ruas",
            category = "Navegação Urbana & Rotas",
            icon = Icons.Default.Map,
            hint = "Detalhes minúsculos de estações, linhas de conexão e referências",
            sections = listOf(
                SampleSection(
                    header = "ESTAÇÃO CENTRAL DE INTEGRAÇÃO (TERMINAL 3)",
                    text = "Linha 1-Azul (Sentido Jabaquara / Tucuruvi) - Acesso Rampa Sul\nLinha 4-Amarela (Sentido Vila Sônia / Luz) - Esteiras Rolantes Nível -2\nLinha 2-Verde (Sentido Vila Madalena / Vila Prudente) - Conexão subterrânea 180m\nHorário de pico: Trens a cada 92 segundos | Elevadores acessíveis na Plataforma B"
                ),
                SampleSection(
                    header = "REFERÊNCIAS URBANAS E RUAS ADJACENTES",
                    text = "R. Barão de Itapetininga, 240 a 380 (Calçadão de Pedestres)\nAv. São João, 1100 (Cruzamento com Viaduto Santa Ifigênia)\nPraça Ramos de Azevedo (Monumento a Carlos Gomes - Ponto de Táxi 24h)\nFarmácia de Plantão: R. 24 de Maio, 88 (Acesso 24 Horas)",
                    isMicroText = true
                ),
                SampleSection(
                    header = "COORDENADAS E ORIENTAÇÃO GPS",
                    text = "LAT: 23°32'51.2\"S | LONG: 46°38'24.8\"W | ELEV: 760m | AZIMUTE 142° SE | Declinação Magnética: -21°30' W",
                    isMicroText = true
                )
            )
        ),
        InspectionSample(
            id = "web",
            title = "Termos Web & Permissões Chrome",
            category = "Navegador & Termos de Uso",
            icon = Icons.Default.Description,
            hint = "Cláusulas contratuais em letras minúsculas e avisos de cookies/permissão",
            sections = listOf(
                SampleSection(
                    header = "POLÍTICA DE PRIVACIDADE E COOKIES DE NAVEGAÇÃO",
                    text = "Ao acessar este portal, cookies de telemetria analítica estritamente necessários são armazenados temporariamente na memória cache do dispositivo conforme LGPD Art. 7º. Nenhum identificador biométrico é transmitido a terceiros sem consentimento explícito e revogável a qualquer momento através do menu de configurações avançadas do navegador."
                ),
                SampleSection(
                    header = "TERMOS DE LICENÇA E RESPONSABILIDADE LIMITADA",
                    text = "CLÁUSULA 14.3: O SOFTWARE É FORNECIDO 'NO ESTADO EM QUE SE ENCONTRA', SEM GARANTIAS IMPLÍCITAS DE COMERCIALIZAÇÃO OU ADEQUAÇÃO A FINALIDADE ESPECÍFICA. EM NENHUMA HIPÓTESE A LICENCIANTE SERÁ RESPONSÁVEL POR LUCROS CESSANTES OU INTERRUPÇÃO DE OPERAÇÕES DECORRENTES DO USO OU DA IMPOSSIBILIDADE DE USO DESTE SERVIÇO.",
                    isMicroText = true
                ),
                SampleSection(
                    header = "PERMISSÕES DE HARDWARE DO DISPOSITIVO (CHROME)",
                    text = "O site pode solicitar acesso à câmera ou geolocalização. A lupa interna e a alça flutuante são programadas com toques transparentes para que você possa clicar livremente em 'Permitir' nas janelas nativas de segurança do Android e Chrome sem nenhum bloqueio de sobreposição.",
                    isMicroText = true
                )
            )
        )
    )
}
