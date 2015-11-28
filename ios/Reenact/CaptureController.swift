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
    
    var originalPhoto: UIImage?
    var newPhoto: UIImage?
    
    let captureSession = AVCaptureSession()
    
    var captureDevice: AVCaptureDevice?
    var captureDevices: [AVCaptureDevice] = []
    var cameraIndex: Int = 0
    var deviceInput: AVCaptureDeviceInput?
    let stillImageOutput = AVCaptureStillImageOutput()
    
    // UI Elements
    let originalPhotoOverlay: UIImageView = UIImageView()
    let captureButton: UIButton = UIButton()
    let switchCameraButton: UIButton = UIButton()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let devices = AVCaptureDevice.devices()
        
        for device in devices {
            // Make sure this particular device supports video (which apparently implies support for photos)
            
            if (device.hasMediaType(AVMediaTypeVideo)) {
                captureDevices.append(device as! AVCaptureDevice)
            }
        }

        // Add the original image overlay
        originalPhotoOverlay.image = originalPhoto
        originalPhotoOverlay.contentMode = .ScaleAspectFit
        originalPhotoOverlay.alpha = 0.75
        
        if (view.bounds.size.width < view.bounds.size.height) {
            // Portrait orientation.
            originalPhotoOverlay.frame = CGRect(
                x: 0,
                y: 0,
                width: view.bounds.width,
                height: view.bounds.height - 100
            )
        }
        else {
            // Landscape orientation.
            originalPhotoOverlay.frame = CGRect(
                x: 0,
                y: 0,
                width: view.bounds.width - 100,
                height: view.bounds.height
            )
        }
        
        view.addSubview(originalPhotoOverlay)
        
        let buttonContainerSize = 100
        
        // Add capture button.
        let captureButtonImage = UIImage(named: "camera.png")
        let captureButtonSize = buttonContainerSize
        captureButton.setImage(captureButtonImage, forState: .Normal)
        captureButton.contentMode = .ScaleAspectFit
        
        if (view.bounds.size.width < view.bounds.size.height) {
            // Portrait orientation.

            captureButton.frame = CGRect(
                x: Int(round(view.bounds.width / 2) - round(CGFloat(captureButtonSize) / 2)),
                y: Int(view.bounds.height - CGFloat(buttonContainerSize)),
                width: captureButtonSize,
                height: captureButtonSize
            )
        }
        else {
            // Landscape
            captureButton.frame = CGRect(
                x: Int(view.bounds.width - CGFloat(buttonContainerSize)),
                y: Int(round(view.bounds.height / 2) - round(CGFloat(captureButtonSize) / 2)),
                width: captureButtonSize,
                height: captureButtonSize
            )
        }
        
        captureButton.addTarget(self, action:"takePicture:", forControlEvents: .TouchUpInside)
        view.addSubview(captureButton)
        
        if captureDevices.count > 1 {
            // Add switch button.
            let switchCameraButtonImage = UIImage(named: "camera-switch.png")
            let switchCameraButtonSize = 80
            switchCameraButton.setImage(switchCameraButtonImage, forState: .Normal)
            switchCameraButton.contentMode = .ScaleAspectFit
            
            if (view.bounds.size.width < view.bounds.size.height) {
                // Portrait orientation.
                switchCameraButton.frame = CGRect(
                    x: Int(round(view.bounds.width / 6 * 5) - round(CGFloat(switchCameraButtonSize) / 2)),
                    y: Int(
                        view.bounds.height -
                            CGFloat(buttonContainerSize) +
                            round(CGFloat(buttonContainerSize - switchCameraButtonSize) / 2)
                    ),
                    width: switchCameraButtonSize,
                    height: switchCameraButtonSize
                )
            }
            else {
                // Landscape
                switchCameraButton.frame = CGRect(
                    x: Int(
                        view.bounds.width -
                        CGFloat(buttonContainerSize) +
                        round(CGFloat(buttonContainerSize - switchCameraButtonSize) / 2)
                    ),
                    y: Int(round(view.bounds.height / 6 * 5) - round(CGFloat(switchCameraButtonSize) / 2)),
                    width: switchCameraButtonSize,
                    height: switchCameraButtonSize
                )
            }
            
            switchCameraButton.addTarget(self, action:"switchCamera:", forControlEvents: .TouchUpInside)
            view.addSubview(switchCameraButton)
        }
        
        // Start fading the overlay image.
        startImageFade()
        
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
        captureSession.sessionPreset = AVCaptureSessionPresetLow

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
    
    func takePicture(sender: UIButton!) {
        print("Taking a picture.")
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
    
    func switchCamera(sender: UIButton!) {
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

