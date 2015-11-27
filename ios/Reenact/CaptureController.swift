//
//  CaptureController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit
import AVFoundation

class CaptureController: UIViewController {
    // MARK: Properties
    
    @IBOutlet weak var originalPhotoOverlay: UIImageView!
    @IBOutlet weak var cameraPreviewContainer: UIView!

    var originalPhoto: UIImage?
    let captureSession = AVCaptureSession()
    
    var captureDevice: AVCaptureDevice?
    var captureDevices: [AVCaptureDevice] = []
    var cameraIndex: Int = 0
    var deviceInput: AVCaptureDeviceInput?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        print("Capture Controller did load")
        // Set originalPhotoOverlay to contain the chosen image.
        originalPhotoOverlay.image = self.originalPhoto
        print("Set image")
        print(self.originalPhoto)
        
        captureSession.sessionPreset = AVCaptureSessionPresetLow
        let devices = AVCaptureDevice.devices()

        print(devices)
        
        for device in devices {
            // Make sure this particular device supports video (which apparently implies support for photos)
            
            if (device.hasMediaType(AVMediaTypeVideo)) {
                captureDevices.append(device as! AVCaptureDevice)
            }
        }
        
        if captureDevices.count != 0 {
            beginSession()
        }
        else {
            print("No capture device.")
        }
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    func getSelectedCamera() -> AVCaptureDevice {
        return captureDevices[cameraIndex]
    }
    
    func beginSession() {
        captureDevice = getSelectedCamera()
        
        do {
            try deviceInput = AVCaptureDeviceInput(device: captureDevice)
        } catch _ {
            print( "That didn't work. :(");
            return
        }

        captureSession.addInput(deviceInput)

        let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession)
        self.cameraPreviewContainer.layer.addSublayer(previewLayer)
        previewLayer.frame = self.cameraPreviewContainer.layer.frame
        captureSession.startRunning()
    }
    
    func endSession() {
        captureSession.removeInput(deviceInput)
        
        captureSession.stopRunning()
    }
    
    // MARK: Actions
    
    @IBAction func takePicture(sender: UIButton) {
    }
    
    @IBAction func switchCamera(sender: UIButton) {
        print("Switching camera")
        endSession()
        print("Ended session")
        cameraIndex++
        cameraIndex %= captureDevices.count
        print("Camera Index is now")
        print(cameraIndex)
        beginSession()
        
    }
    
    // MARK: Delegates
    
}

