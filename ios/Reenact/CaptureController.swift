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

    var originalPhoto: UIImage?
    var newPhoto: UIImage?
    
    let captureSession = AVCaptureSession()
    
    var captureDevice: AVCaptureDevice?
    var captureDevices: [AVCaptureDevice] = []
    var cameraIndex: Int = 0
    var deviceInput: AVCaptureDeviceInput?
    let stillImageOutput = AVCaptureStillImageOutput()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        
        print("Capture Controller did load")
        // Set originalPhotoOverlay to contain the chosen image.
        originalPhotoOverlay.image = self.originalPhoto
        startImageFade()
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
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject!) {
        if (segue.identifier == "captureToConfirm") {
            print("In prepareForSegue")
            
            let svc = segue.destinationViewController as! ConfirmController;
            
            svc.originalPhoto = self.originalPhoto
            svc.newPhoto = self.newPhoto
        }
    }
    
    func startImageFade() {
        fadeOut()
    }
    
    func fadeOut() {
        UIView.animateWithDuration( 5.0, delay: 0, options: UIViewAnimationOptions.CurveEaseOut, animations : {
            self.originalPhotoOverlay.alpha = 0;
            }, completion : {
                (finished: Bool) -> Void in
                self.fadeIn()
            }
        )
    }
    
    func fadeIn() {
        UIView.animateWithDuration( 5.0, delay: 0, options: UIViewAnimationOptions.CurveEaseIn, animations : {
            self.originalPhotoOverlay.alpha = 0.75
            }, completion : {
                (finished: Bool) -> Void in
                self.fadeOut()
            }
        )
        
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

        
        let bounds = self.originalPhotoOverlay.layer.bounds;
        
        captureSession.sessionPreset = AVCaptureSessionPresetPhoto
        captureSession.startRunning()
        stillImageOutput.outputSettings = [AVVideoCodecKey:AVVideoCodecJPEG]
        if captureSession.canAddOutput(stillImageOutput) {
            captureSession.addOutput(stillImageOutput)
        }
        if let previewLayer = AVCaptureVideoPreviewLayer(session: captureSession) {
            previewLayer.bounds = CGRectMake(0.0, 0.0, bounds.size.width, bounds.size.height)
            previewLayer.position = CGPointMake(bounds.midX, bounds.midY)
            previewLayer.videoGravity = AVLayerVideoGravityResizeAspectFill
            let cameraPreview = UIView(frame: CGRectMake(0.0, 0.0, bounds.size.width, bounds.size.height))
            cameraPreview.layer.addSublayer(previewLayer)
//            cameraPreview.addGestureRecognizer(UITapGestureRecognizer(target: self, action:"saveToCamera:"))
            view.addSubview(cameraPreview)
            view.sendSubviewToBack(cameraPreview)
        }
        
        
        
        
        
        
    }
    
    func endSession() {
        captureSession.removeInput(deviceInput)
        
        captureSession.stopRunning()
    }
    
    // MARK: Actions
    
    @IBAction func takePicture(sender: UIButton) {
        if let videoConnection = stillImageOutput.connectionWithMediaType(AVMediaTypeVideo) {
            stillImageOutput.captureStillImageAsynchronouslyFromConnection(videoConnection) {
                (imageDataSampleBuffer, error) -> Void in
                let imageData = AVCaptureStillImageOutput.jpegStillImageNSDataRepresentation(imageDataSampleBuffer)
                self.newPhoto = UIImage(data: imageData)
                
                
                print(self.newPhoto)
                
                self.performSegueWithIdentifier("captureToConfirm", sender: self)
            }
        }
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

