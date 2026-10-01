import UIKit
import ImageIO
import shared

/// Decoding, EXIF orientation handling, cropping and scaling for picked/captured images.
enum ImagePipeline {

    /// Pixel size of the image after applying its EXIF orientation.
    static func orientedSize(of data: Data) -> CGSize? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil),
              let props = CGImageSourceCopyPropertiesAtIndex(source, 0, nil) as? [CFString: Any],
              let width = props[kCGImagePropertyPixelWidth] as? Int,
              let height = props[kCGImagePropertyPixelHeight] as? Int else { return nil }
        let orientation = props[kCGImagePropertyOrientation] as? UInt32 ?? 1
        return (5...8).contains(orientation)
            ? CGSize(width: height, height: width)
            : CGSize(width: width, height: height)
    }

    /// Oriented image whose longer edge is at most `maxEdge` pixels (scale 1, so points == pixels).
    static func displayImage(from data: Data, maxEdge: Int = 2048) -> UIImage? {
        guard let size = orientedSize(of: data),
              let cgImage = thumbnail(of: data, maxPixel: min(maxEdge, Int(max(size.width, size.height)))) else { return nil }
        return UIImage(cgImage: cgImage, scale: 1, orientation: .up)
    }

    /// Crops `rect` (fractions of the oriented image) out of the original, scales it to `output` and encodes JPEG.
    static func cropAndScale(data: Data, rect: CropRect, output: CropOutputSize, quality: Int) -> Data? {
        guard let full = orientedSize(of: data) else { return nil }
        let cropW = CGFloat(rect.width) * full.width
        let cropH = CGFloat(rect.height) * full.height
        // Decode only as large as needed so the crop still has at least the output resolution.
        let needed = min(1, max(CGFloat(output.width) / cropW, CGFloat(output.height) / cropH))
        let maxPixel = Int((max(full.width, full.height) * needed).rounded(.up))
        guard let decoded = thumbnail(of: data, maxPixel: maxPixel) else { return nil }

        let w = CGFloat(decoded.width), h = CGFloat(decoded.height)
        let cropRect = CGRect(
            x: CGFloat(rect.left) * w, y: CGFloat(rect.top) * h,
            width: CGFloat(rect.width) * w, height: CGFloat(rect.height) * h
        ).integral.intersection(CGRect(x: 0, y: 0, width: w, height: h))
        guard let cropped = decoded.cropping(to: cropRect) else { return nil }

        let format = UIGraphicsImageRendererFormat()
        format.scale = 1
        format.opaque = true
        let size = CGSize(width: Int(output.width), height: Int(output.height))
        let image = UIGraphicsImageRenderer(size: size, format: format).image { _ in
            UIImage(cgImage: cropped).draw(in: CGRect(origin: .zero, size: size))
        }
        return image.jpegData(compressionQuality: CGFloat(quality) / 100)
    }

    private static func thumbnail(of data: Data, maxPixel: Int) -> CGImage? {
        guard let source = CGImageSourceCreateWithData(data as CFData, nil) else { return nil }
        let options: [CFString: Any] = [
            kCGImageSourceCreateThumbnailFromImageAlways: true,
            kCGImageSourceCreateThumbnailWithTransform: true, // applies EXIF orientation
            kCGImageSourceShouldCacheImmediately: true,
            kCGImageSourceThumbnailMaxPixelSize: max(1, maxPixel)
        ]
        return CGImageSourceCreateThumbnailAtIndex(source, 0, options as CFDictionary)
    }
}
