package com.avtar.cabbilling.data.model

/** A selectable place: a city/hub tagged with its state or UT. */
data class IndiaLocation(val city: String, val state: String) {
    val label: String get() = if (state.isBlank()) city else "$city, $state"
}

/**
 * Static, offline catalogue of major Indian cities and transit hubs, grouped by
 * state/UT. Used to power the location pickers; users can also type a fully
 * custom route that isn't in this list.
 */
object IndiaLocations {

    val byState: Map<String, List<String>> = linkedMapOf(
        "Andhra Pradesh" to listOf(
            "Visakhapatnam", "Vijayawada", "Guntur", "Tirupati", "Nellore", "Kurnool",
            "Rajahmundry", "Kadapa", "Anantapur", "Kakinada", "Eluru", "Ongole",
            "Chittoor", "Vizianagaram", "Srikakulam", "Machilipatnam", "Amaravati"
        ),
        "Arunachal Pradesh" to listOf(
            "Itanagar", "Naharlagun", "Tawang", "Pasighat", "Ziro", "Bomdila"
        ),
        "Assam" to listOf(
            "Guwahati", "Dibrugarh", "Silchar", "Jorhat", "Nagaon", "Tinsukia",
            "Tezpur", "Dhubri", "Bongaigaon", "Sivasagar", "Diphu"
        ),
        "Bihar" to listOf(
            "Patna", "Gaya", "Bhagalpur", "Muzaffarpur", "Darbhanga", "Purnia",
            "Bihar Sharif", "Arrah", "Begusarai", "Katihar", "Chapra", "Motihari",
            "Hajipur", "Sasaram"
        ),
        "Chhattisgarh" to listOf(
            "Raipur", "Bhilai", "Bilaspur", "Korba", "Durg", "Raigarh",
            "Jagdalpur", "Ambikapur", "Rajnandgaon"
        ),
        "Goa" to listOf(
            "Panaji", "Margao", "Vasco da Gama", "Mapusa", "Ponda", "Calangute"
        ),
        "Gujarat" to listOf(
            "Ahmedabad", "Surat", "Vadodara", "Rajkot", "Bhavnagar", "Jamnagar",
            "Gandhinagar", "Junagadh", "Anand", "Nadiad", "Morbi", "Bharuch",
            "Navsari", "Gandhidham", "Porbandar", "Vapi", "Mehsana", "Bhuj"
        ),
        "Haryana" to listOf(
            "Gurugram", "Faridabad", "Panipat", "Ambala", "Yamunanagar", "Rohtak",
            "Hisar", "Karnal", "Sonipat", "Panchkula", "Bhiwani", "Sirsa",
            "Bahadurgarh", "Kurukshetra", "Rewari"
        ),
        "Himachal Pradesh" to listOf(
            "Shimla", "Manali", "Dharamshala", "Solan", "Mandi", "Kullu",
            "Palampur", "Baddi", "Bilaspur", "Hamirpur", "Una", "Kasauli", "Dalhousie"
        ),
        "Jharkhand" to listOf(
            "Ranchi", "Jamshedpur", "Dhanbad", "Bokaro Steel City", "Deoghar",
            "Hazaribagh", "Giridih", "Ramgarh", "Phusro"
        ),
        "Karnataka" to listOf(
            "Bengaluru", "Mysuru", "Hubballi", "Mangaluru", "Belagavi", "Kalaburagi",
            "Davanagere", "Ballari", "Vijayapura", "Shivamogga", "Tumakuru", "Udupi",
            "Hassan", "Bidar", "Raichur", "Hospet", "Chikkamagaluru"
        ),
        "Kerala" to listOf(
            "Thiruvananthapuram", "Kochi", "Kozhikode", "Thrissur", "Kollam", "Kannur",
            "Alappuzha", "Palakkad", "Kottayam", "Malappuram", "Kasaragod",
            "Pathanamthitta", "Munnar", "Ernakulam"
        ),
        "Madhya Pradesh" to listOf(
            "Bhopal", "Indore", "Jabalpur", "Gwalior", "Ujjain", "Sagar", "Dewas",
            "Satna", "Ratlam", "Rewa", "Katni", "Singrauli", "Burhanpur", "Khandwa",
            "Chhindwara", "Vidisha"
        ),
        "Maharashtra" to listOf(
            "Mumbai", "Pune", "Nagpur", "Nashik", "Thane", "Chhatrapati Sambhajinagar",
            "Solapur", "Kolhapur", "Amravati", "Navi Mumbai", "Nanded", "Sangli",
            "Jalgaon", "Akola", "Latur", "Ahmednagar", "Chandrapur", "Satara",
            "Ratnagiri", "Panvel", "Lonavala"
        ),
        "Manipur" to listOf("Imphal", "Thoubal", "Bishnupur", "Churachandpur"),
        "Meghalaya" to listOf("Shillong", "Tura", "Jowai", "Nongpoh"),
        "Mizoram" to listOf("Aizawl", "Lunglei", "Champhai"),
        "Nagaland" to listOf("Kohima", "Dimapur", "Mokokchung"),
        "Odisha" to listOf(
            "Bhubaneswar", "Cuttack", "Rourkela", "Berhampur", "Sambalpur", "Puri",
            "Balasore", "Baripada", "Jharsuguda", "Angul"
        ),
        "Punjab" to listOf(
            "Ludhiana", "Amritsar", "Jalandhar", "Patiala", "Bathinda", "Mohali",
            "Hoshiarpur", "Pathankot", "Moga", "Firozpur", "Kapurthala", "Barnala",
            "Phagwara"
        ),
        "Rajasthan" to listOf(
            "Jaipur", "Jodhpur", "Udaipur", "Kota", "Ajmer", "Bikaner", "Bhilwara",
            "Alwar", "Sikar", "Pali", "Sri Ganganagar", "Bharatpur", "Jaisalmer",
            "Mount Abu", "Pushkar", "Chittorgarh", "Nagaur"
        ),
        "Sikkim" to listOf("Gangtok", "Namchi", "Gyalshing", "Mangan"),
        "Tamil Nadu" to listOf(
            "Chennai", "Coimbatore", "Madurai", "Tiruchirappalli", "Salem",
            "Tirunelveli", "Tiruppur", "Erode", "Vellore", "Thoothukudi", "Dindigul",
            "Thanjavur", "Nagercoil", "Kanchipuram", "Karur", "Hosur", "Cuddalore",
            "Udhagamandalam (Ooty)", "Rameswaram"
        ),
        "Telangana" to listOf(
            "Hyderabad", "Secunderabad", "Warangal", "Nizamabad", "Karimnagar",
            "Khammam", "Ramagundam", "Mahbubnagar", "Nalgonda", "Siddipet"
        ),
        "Tripura" to listOf("Agartala", "Udaipur", "Dharmanagar", "Kailashahar"),
        "Uttar Pradesh" to listOf(
            "Lucknow", "Kanpur", "Agra", "Varanasi", "Prayagraj", "Ghaziabad",
            "Noida", "Greater Noida", "Meerut", "Bareilly", "Aligarh", "Moradabad",
            "Gorakhpur", "Saharanpur", "Firozabad", "Jhansi", "Muzaffarnagar",
            "Mathura", "Ayodhya", "Rae Bareli", "Sultanpur", "Etawah"
        ),
        "Uttarakhand" to listOf(
            "Dehradun", "Haridwar", "Rishikesh", "Haldwani", "Roorkee", "Nainital",
            "Rudrapur", "Kashipur", "Mussoorie", "Almora", "Pithoragarh", "Kotdwar"
        ),
        "West Bengal" to listOf(
            "Kolkata", "Howrah", "Durgapur", "Asansol", "Siliguri", "Darjeeling",
            "Kharagpur", "Malda", "Bardhaman", "Haldia", "Krishnanagar", "Jalpaiguri",
            "Kalimpong", "Digha"
        ),
        "Delhi (NCT)" to listOf(
            "New Delhi", "Delhi", "Connaught Place", "Dwarka", "Rohini", "Saket",
            "Karol Bagh", "Nehru Place", "IGI Airport"
        ),
        "Jammu & Kashmir" to listOf(
            "Srinagar", "Jammu", "Anantnag", "Baramulla", "Udhampur", "Katra",
            "Sopore", "Gulmarg", "Pahalgam", "Sonamarg"
        ),
        "Ladakh" to listOf("Leh", "Kargil", "Nubra", "Diskit"),
        "Chandigarh" to listOf("Chandigarh"),
        "Puducherry" to listOf("Puducherry", "Karaikal", "Yanam", "Mahe"),
        "Andaman & Nicobar Islands" to listOf("Port Blair", "Havelock Island"),
        "Dadra & Nagar Haveli and Daman & Diu" to listOf("Daman", "Silvassa", "Diu"),
        "Lakshadweep" to listOf("Kavaratti", "Agatti")
    )

    /** Flattened, de-duplicated catalogue used for searching. */
    val all: List<IndiaLocation> = byState.entries
        .flatMap { (state, cities) -> cities.map { IndiaLocation(it, state) } }

    /** Case-insensitive contains-match over both city and state. */
    fun search(query: String): List<IndiaLocation> {
        val q = query.trim()
        if (q.isEmpty()) return all
        return all.filter { it.city.contains(q, ignoreCase = true) || it.state.contains(q, ignoreCase = true) }
    }
}
