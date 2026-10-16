import SwiftUI

/// A hexagon with its corners at the left and right, like the achievement badges of fitness apps (same as Android).
struct HexagonShape: Shape {
    func path(in rect: CGRect) -> Path {
        var path = Path()
        let radiusX = rect.width / 2
        let radiusY = rect.height / 2
        for i in 0..<6 {
            let angle = Double.pi / 3 * Double(i)
            let point = CGPoint(x: rect.midX + radiusX * cos(angle), y: rect.midY + radiusY * sin(angle))
            if i == 0 { path.move(to: point) } else { path.addLine(to: point) }
        }
        path.closeSubpath()
        return path
    }
}

#Preview("Light Mode") {
    HexagonShape().fill(Color.wandrPrimary).frame(width: 88, height: 88).padding().preferredColorScheme(.light)
}

#Preview("Dark Mode") {
    HexagonShape().stroke(Color.wandrPrimary, lineWidth: 3).frame(width: 88, height: 88).padding().preferredColorScheme(.dark)
}

#Preview("Large", traits: .fixedLayout(width: 300, height: 300)) {
    HexagonShape().fill(Color.wandrSecondary).padding()
}
