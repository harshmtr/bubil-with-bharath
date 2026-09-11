package com.example.domain.models

data class SamplePreset(
    val title: String,
    val subtitle: String,
    val category: ProductCategory,
    val rawText: String,
    val expectedTrustScore: Int
)

object SamplePresets {
    val list = listOf(
        SamplePreset(
            title = "Butter Cookies (Compliant Food)",
            subtitle = "Britannia • 100g • Complete Legal Metrology",
            category = ProductCategory.FOOD,
            expectedTrustScore = 100,
            rawText = """
                BRITANNIA
                Good Day Butter Cookies
                100% Vegetarian Green Dot
                FSSAI Lic. No. 10015043001129
                Net Weight: 100g
                MRP: ₹35.00 (Incl. of all taxes)
                Date of Pkg: 10/01/2026
                Best Before: 10/10/2026
                Contains: Wheat (Gluten), Milk, Soya.
                Nutrition per 100g: Energy 490 kcal, Sugar 22g, Protein 7g
                Manufactured by: Britannia Industries Ltd, 5/1A Hungerford Street, Kolkata - 700017
                Consumer Care Helpline: 1800-425-4449 or feedback@britannia.co.in
            """.trimIndent()
        ),
        SamplePreset(
            title = "Forest Honey (MRP & FSSAI Violation)",
            subtitle = "Organic Forest • Missing Price & License",
            category = ProductCategory.FOOD,
            expectedTrustScore = 40,
            rawText = """
                HIMALAYAN FOREST HONEY
                Pure Wild Organic Honey
                Net Wt: 250g
                Best Before: 12/2027
                Manufactured by: Valley Produce Traders, Dehradun, Uttarakhand
                (No MRP printed on label - Dealer overcharging risk)
                (No 14-digit FSSAI food license found)
            """.trimIndent()
        ),
        SamplePreset(
            title = "Amoxicillin Tablets (Schedule H Violation)",
            subtitle = "Antibiotic 625mg • Missing Rx Warning",
            category = ProductCategory.MEDICINE,
            expectedTrustScore = 45,
            rawText = """
                AMOX-CLAV 625
                Amoxicillin and Potassium Clavulanate Tablets IP
                Each film coated tablet contains:
                Amoxicillin Trihydrate IP eq. to Amoxicillin 500mg
                Diluted Potassium Clavulanate IP eq. to Clavulanic Acid 125mg
                Net Qty: 10 Tablets
                MRP: ₹190.00
                Expiry: 09/2027
                Mfg by: Baddi BioPharma Pvt Ltd, Solan, Himachal Pradesh
                Dosage: As directed by the physician
                (Missing Statutory Red Box Schedule H Warning)
            """.trimIndent()
        ),
        SamplePreset(
            title = "Paracetamol Suspension (Compliant Medicine)",
            subtitle = "Apex Pharma • 60ml • Full Schedule H Warning",
            category = ProductCategory.MEDICINE,
            expectedTrustScore = 100,
            rawText = """
                APEX PHARMA
                Paracetamol Paediatric Oral Suspension IP
                Composition: Each 5ml contains Paracetamol IP 250mg
                SCHEDULE H PRESCRIPTION DRUG - CAUTION:
                Warning: To be sold by retail on the prescription of a Registered Medical Practitioner only.
                Net Volume: 60ml
                MRP: ₹48.50 (Inclusive of all taxes)
                Mfg Date: 01/2026, Expiry: 12/2028
                Mfg by: Apex Laboratories Pvt Ltd, Alathur, Chennai - 603110
                Consumer Care Helpline: 044-24991234
            """.trimIndent()
        )
    )
}
