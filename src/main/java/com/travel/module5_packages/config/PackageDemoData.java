package com.travel.module5_packages.config;

import com.travel.module5_packages.entity.Itinerary;
import com.travel.module5_packages.entity.TourPackage;
import com.travel.module5_packages.repository.TourPackageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;

@Configuration
public class PackageDemoData {

    private static final Logger log = LoggerFactory.getLogger(PackageDemoData.class);

    @Bean
    public CommandLineRunner seedPackages(TourPackageRepository repository, TransactionTemplate tx,
                                          @Value("${app.demo-data:true}") boolean enabled) {
        return args -> {
            if (!enabled || repository.count() > 0) return;
            tx.executeWithoutResult(status -> {
                repository.save(pkg("Buddhist Pilgrimage to India & Nepal", "Pilgrimage", "India & Nepal", 8, "285000",
                        "Walk in the footsteps of the Buddha with a guided group pilgrimage to the four great sacred sites, "
                                + "with Sinhala-speaking guides, vegetarian meals and daily observances.",
                        day(1, "Colombo to Bodh Gaya", "Flight to Gaya, transfer to Bodh Gaya, evening at the Mahabodhi Temple", "Hotel near Mahabodhi Temple", "Flight + coach"),
                        day(2, "Bodh Gaya", "Morning pooja under the Bodhi tree, visit Sujata Stupa and Dungeshwari caves", "Hotel near Mahabodhi Temple", "Coach"),
                        day(3, "Rajgir & Nalanda", "Vulture's Peak, Venuvana and the ruins of Nalanda University", "Rajgir hotel", "Coach"),
                        day(4, "Varanasi & Sarnath", "Dhamek Stupa, Mulagandha Kuti Vihara and the evening Ganga aarti", "Varanasi hotel", "Coach"),
                        day(5, "Kushinagar", "Mahaparinirvana Temple and Ramabhar Stupa", "Kushinagar hotel", "Coach"),
                        day(6, "Lumbini, Nepal", "Maya Devi Temple, Ashoka Pillar and the monastic zone", "Lumbini hotel", "Coach (border crossing)"),
                        day(7, "Sravasti", "Jetavana Monastery and the Anathapindika Stupa", "Sravasti hotel", "Coach"),
                        day(8, "Return to Colombo", "Transfer to Lucknow airport and flight home", null, "Coach + flight")));
                repository.save(pkg("Golden Triangle Explorer", "Cultural", "North India", 6, "210000",
                        "Delhi, Agra and Jaipur in one easy week: Mughal monuments, Rajput forts, bazaars and a sunrise at the Taj Mahal.",
                        day(1, "Delhi", "Arrival, Qutub Minar and India Gate drive", "4-star hotel, Delhi", "Flight + private car"),
                        day(2, "Delhi", "Old Delhi rickshaw ride, Jama Masjid, Red Fort, Humayun's Tomb", "4-star hotel, Delhi", "Private car"),
                        day(3, "Agra", "Drive to Agra, Agra Fort and Mehtab Bagh at sunset", "4-star hotel, Agra", "Private car"),
                        day(4, "Agra to Jaipur", "Taj Mahal at sunrise, Fatehpur Sikri on the way to Jaipur", "4-star hotel, Jaipur", "Private car"),
                        day(5, "Jaipur", "Amber Fort, City Palace, Hawa Mahal and Johari Bazaar", "4-star hotel, Jaipur", "Private car"),
                        day(6, "Return", "Transfer to Jaipur airport", null, "Flight")));
                repository.save(pkg("Kerala Backwaters Escape", "Nature", "Kerala, India", 5, "165000",
                        "Tea hills, spice gardens and a night on a traditional houseboat - a slow, green holiday in God's Own Country.",
                        day(1, "Kochi", "Fort Kochi walk, Chinese fishing nets and a Kathakali show", "Heritage hotel, Kochi", "Flight + private car"),
                        day(2, "Munnar", "Drive into the hills, tea museum and plantation walk", "Hill resort, Munnar", "Private car"),
                        day(3, "Munnar", "Eravikulam National Park and Mattupetty Dam", "Hill resort, Munnar", "Private car"),
                        day(4, "Alleppey", "Board a houseboat and cruise the backwaters", "Houseboat", "Private car + houseboat"),
                        day(5, "Return", "Disembark and transfer to Kochi airport", null, "Private car + flight")));
                repository.save(pkg("Sri Lanka Cultural Triangle", "Cultural", "Sri Lanka", 4, "95000",
                        "Ancient capitals, rock fortresses and the Temple of the Tooth - the classic heritage circuit for visiting friends and family.",
                        day(1, "Anuradhapura", "Sri Maha Bodhi, Ruwanwelisaya and Jetavanaramaya", "Hotel, Anuradhapura", "Air-conditioned van"),
                        day(2, "Sigiriya & Dambulla", "Climb Sigiriya rock, Dambulla cave temple", "Hotel, Sigiriya", "Air-conditioned van"),
                        day(3, "Polonnaruwa", "Gal Vihara, the royal palace and the Parakrama Samudra", "Hotel, Sigiriya", "Air-conditioned van"),
                        day(4, "Kandy", "Spice garden, Temple of the Tooth and return to Colombo", null, "Air-conditioned van")));
                repository.save(pkg("Dubai City & Desert", "City", "United Arab Emirates", 5, "245000",
                        "Skyscrapers, souks and dunes: Burj Khalifa, a dhow dinner cruise and an evening desert safari.",
                        day(1, "Dubai", "Arrival and Dubai Marina walk", "4-star hotel, Dubai", "Flight + transfer"),
                        day(2, "Dubai", "City tour, Burj Khalifa observation deck and Dubai Mall", "4-star hotel, Dubai", "Coach"),
                        day(3, "Desert", "Dune bashing, camel ride and BBQ dinner at a desert camp", "4-star hotel, Dubai", "4x4"),
                        day(4, "Abu Dhabi", "Sheikh Zayed Grand Mosque and Louvre Abu Dhabi", "4-star hotel, Dubai", "Coach"),
                        day(5, "Return", "Gold and spice souks, transfer to the airport", null, "Transfer + flight")));
                repository.save(pkg("Thailand Island Hopper", "Beach", "Thailand", 7, "260000",
                        "Bangkok's temples, then Phuket and the Phi Phi islands - beaches, boat trips and night markets.",
                        day(1, "Bangkok", "Arrival and evening at Asiatique riverfront", "Hotel, Bangkok", "Flight + transfer"),
                        day(2, "Bangkok", "Grand Palace, Wat Pho and Wat Arun", "Hotel, Bangkok", "Coach + river boat"),
                        day(3, "Phuket", "Fly to Phuket, Patong beach", "Beach resort, Phuket", "Flight + transfer"),
                        day(4, "Phi Phi Islands", "Speedboat tour, Maya Bay and snorkelling", "Beach resort, Phuket", "Speedboat"),
                        day(5, "Phuket", "Big Buddha, Old Town and a Thai cooking class", "Beach resort, Phuket", "Coach"),
                        day(6, "Phuket", "Free day at leisure", "Beach resort, Phuket", null),
                        day(7, "Return", "Transfer to Phuket airport", null, "Transfer + flight")));
            });
            log.info("Added sample tour packages for the website (app.demo-data=true)");
        };
    }

    private static TourPackage pkg(String name, String category, String destination, int days, String price,
                                   String description, Itinerary... itinerary) {
        TourPackage p = new TourPackage();
        p.setPackageName(name);
        p.setCategory(category);
        p.setDestination(destination);
        p.setDuration(days);
        p.setPrice(new BigDecimal(price));
        p.setStatus("AVAILABLE");
        p.setDescription(description);
        for (Itinerary i : itinerary) {
            i.setTourPackage(p);
            p.getItineraries().add(i);
        }
        return p;
    }

    private static Itinerary day(int n, String destination, String activities, String accommodation, String transport) {
        Itinerary i = new Itinerary();
        i.setDayNumber(n);
        i.setDestination(destination);
        i.setActivities(activities);
        i.setAccommodation(accommodation);
        i.setTransport(transport);
        return i;
    }
}
