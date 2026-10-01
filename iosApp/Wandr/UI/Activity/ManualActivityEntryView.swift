import SwiftUI

struct ManualActivityEntryView: View {
    @State private var title: String = ""
    @State private var descriptionText: String = ""
    @State private var distanceKm: String = "5.0"
    @State private var durationMinutes: String = "45.0"
    @State private var elevationMeters: String = "120.0"

    var onSubmit: (String, String, Double, Double, Double) -> Void = { _, _, _, _, _ in }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 16) {
                Text(LocalizedStringKey("log_manual_activity_title"))
                    .font(.title2)
                    .bold()

                VStack(spacing: 12) {
                    TextField("Title", text: $title)
                        .textFieldStyle(.roundedBorder)

                    TextField("Description", text: $descriptionText)
                        .textFieldStyle(.roundedBorder)

                    HStack {
                        Text(LocalizedStringKey("distance_label"))
                            .font(.caption)
                        Spacer()
                        TextField("Distance", text: $distanceKm)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 80)
                    }

                    HStack {
                        Text(LocalizedStringKey("duration_label"))
                            .font(.caption)
                        Spacer()
                        TextField("Duration", text: $durationMinutes)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 80)
                    }

                    HStack {
                        Text(LocalizedStringKey("elevation_label"))
                            .font(.caption)
                        Spacer()
                        TextField("Elevation", text: $elevationMeters)
                            .keyboardType(.decimalPad)
                            .multilineTextAlignment(.trailing)
                            .frame(width: 80)
                    }
                }
                .padding()
                .background(.ultraThinMaterial)
                .cornerRadius(12)

                Button(action: {
                    let dist = Double(distanceKm) ?? 0.0
                    let dur = Double(durationMinutes) ?? 0.0
                    let elev = Double(elevationMeters) ?? 0.0
                    onSubmit(title, descriptionText, dist, dur, elev)
                }) {
                    Text(LocalizedStringKey("save_activity_button"))
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 50)
                        .background(Color.blue)
                        .cornerRadius(12)
                }
            }
            .padding(16)
        }
    }
}

#Preview("Light Mode") {
    ManualActivityEntryView()
        .preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    ManualActivityEntryView()
        .preferredColorScheme(.dark)
}
