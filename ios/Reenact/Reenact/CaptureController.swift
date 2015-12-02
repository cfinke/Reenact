//
//  CaptureController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit
import AVFoundation

class CaptureController: ReenactControllerBase {
    // MARK: Properties
    
    var originalPhoto: UIImage!
    var newPhoto: UIImage!
    
    let captureSession = AVCaptureSession()
    
    var captureDevice: AVCaptureDevice!
    var captureDevices: [AVCaptureDevice] = []
    var deviceInput: AVCaptureDeviceInput!
    let stillImageOutput = AVCaptureStillImageOutput()
    
    var cameraIndex: Int = 0
    var captureMethod: String!
    
    // UI Elements
    let originalPhotoOverlay: UIImageView = UIImageView()
    let captureButton: UIButton = UIButton()
    let switchCameraButton: UIButton = UIButton()
    let cancelButton: UIButton = UIButton()
    
    let previewLayer: AVCaptureVideoPreviewLayer = AVCaptureVideoPreviewLayer()
    
    var viewAppeared = false
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        let devices = AVCaptureDevice.devices()
        
        for device in devices {
            // Make sure this particular device supports video (which apparently implies support for photos)
            
            if (device.hasMediaType(AVMediaTypeVideo)) {
                captureDevices.append(device as! AVCaptureDevice)
            }
        }
        
        // Get the last-used camera.
        cameraIndex = NSUserDefaults.standardUserDefaults().integerForKey("cameraIndex")
        
        let storedCaptureMethod = NSUserDefaults.standardUserDefaults().stringForKey("captureMethod")
        
        if nil == storedCaptureMethod {
            captureMethod = "overlay"
        }
        else {
            captureMethod = storedCaptureMethod!
        }
        
        buildLayout(view.bounds.size)
        
        // Start fading the overlay image.
        if !screenshotMode {
            if "overlay" == captureMethod {
                startImageFade()
            }
        }
    }
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        
        if originalPhoto.size.width <= originalPhoto.size.height {
            UIDevice.currentDevice().setValue(UIInterfaceOrientation.Portrait.rawValue, forKey: "orientation")
        }
        else {
            UIDevice.currentDevice().setValue(UIInterfaceOrientation.LandscapeLeft.rawValue, forKey: "orientation")
        }
        
        viewAppeared = true
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject!) {
        if (segue.identifier == "captureToConfirm") {
            let svc = segue.destinationViewController as! ConfirmController;
            
            svc.originalPhoto = self.originalPhoto
            svc.newPhoto = self.newPhoto
        }
    }
    
    override func shouldAutorotate() -> Bool {
        if originalPhoto.size.width <= originalPhoto.size.height {
            if (UIDevice.currentDevice().orientation == UIDeviceOrientation.LandscapeLeft ||
                UIDevice.currentDevice().orientation == UIDeviceOrientation.LandscapeRight ||
                UIDevice.currentDevice().orientation == UIDeviceOrientation.Unknown) {
                return false;
            }
        }
        else {
            if (UIDevice.currentDevice().orientation == UIDeviceOrientation.Portrait ||
                UIDevice.currentDevice().orientation == UIDeviceOrientation.PortraitUpsideDown ||
                UIDevice.currentDevice().orientation == UIDeviceOrientation.Unknown) {
                return false;
            }
        }
            
        return true;
    }
    
    override func supportedInterfaceOrientations() -> UIInterfaceOrientationMask {
        if !viewAppeared {
            return [
                UIInterfaceOrientationMask.Portrait,
                UIInterfaceOrientationMask.PortraitUpsideDown,
                UIInterfaceOrientationMask.LandscapeRight,
                UIInterfaceOrientationMask.LandscapeLeft,
                UIInterfaceOrientationMask.Landscape
            ]
        }
        
        if originalPhoto.size.width <= originalPhoto.size.height {
            return [UIInterfaceOrientationMask.Portrait]
        }
        else {
            return [UIInterfaceOrientationMask.Landscape]
        }
    }
    
    func startImageFade() {
        fadeOut()
    }
    
    func fadeOut() {
        UIView.animateWithDuration( 5.0, delay: 0, options: UIViewAnimationOptions.CurveEaseOut, animations : {
            self.originalPhotoOverlay.alpha = 0.25
            }, completion : {
                (finished: Bool) -> Void in
                self.fadeIn()
            }
        )
    }
    
    func fadeIn() {
        UIView.animateWithDuration( 5.0, delay: 0, options: UIViewAnimationOptions.CurveEaseIn, animations : {
            self.originalPhotoOverlay.alpha = 0.85
            }, completion : {
                (finished: Bool) -> Void in
                self.fadeOut()
            }
        )
        
    }
    
    func getSelectedCamera() -> AVCaptureDevice? {
        if cameraIndex < captureDevices.count {
            return captureDevices[cameraIndex]
        }
        
        return nil
    }
    
    func beginSession(size: CGSize) {
        if screenshotMode {
            let screenshotCameraPreview = UIImageView()
            let screenshotCameraPreviewImage = UIImage(named: screenshotModeOrientation + "-new.jpg")
            screenshotCameraPreview.image = screenshotCameraPreviewImage
            screenshotCameraPreview.contentMode = .ScaleAspectFit
            
            if "overlay" == captureMethod {
                screenshotCameraPreview.frame = originalPhotoOverlay.frame
                
            }
            else if "comparison" == captureMethod {
                if (size.width <= size.height) {
                    // Portrait
                    screenshotCameraPreview.frame = CGRect(
                        x: Int(round(size.width / 2)),
                        y: 0,
                        width: Int(round(size.width / 2)),
                        height: Int(size.height - CGFloat(buttonContainerSize))
                    )
                }
                else {
                    // Landscape
                    screenshotCameraPreview.frame = CGRect(
                        x: 0,
                        y: Int(round(size.height / 2)),
                        width: Int(size.width - CGFloat(buttonContainerSize)),
                        height: Int(round(size.height / 2))
                    )
                }
            }
            
            view.addSubview(screenshotCameraPreview)
            view.sendSubviewToBack(screenshotCameraPreview)
            
            return
        }
        
        captureDevice = getSelectedCamera()
        
        if (nil == captureDevice) {
            cancelCapture(nil)
            return
        }
        
        if captureDevice.position == .Front {
            originalPhotoOverlay.transform = CGAffineTransformMakeScale(-1, 1)
        }
        else {
            originalPhotoOverlay.transform = CGAffineTransformIdentity
        }
        
        do {
            try captureDevice.lockForConfiguration()
            
            if captureDevice.isExposureModeSupported(.ContinuousAutoExposure) {
                captureDevice.exposureMode = .ContinuousAutoExposure
            }
            
            if captureDevice.isFocusModeSupported(.ContinuousAutoFocus) {
                captureDevice.focusMode = .ContinuousAutoFocus
            }
            
            captureDevice.unlockForConfiguration()
        } catch _ {
            // Oh well.
        }
        
        captureSession.sessionPreset = AVCaptureSessionPresetPhoto

        do {
            try deviceInput = AVCaptureDeviceInput(device: captureDevice)
        } catch _ {
            cancelCapture(nil)
            return
        }
        
        captureSession.addInput(deviceInput)


        
        captureSession.sessionPreset = AVCaptureSessionPresetPhoto
        captureSession.startRunning()
        stillImageOutput.outputSettings = [AVVideoCodecKey:AVVideoCodecJPEG]
        if captureSession.canAddOutput(stillImageOutput) {
            captureSession.addOutput(stillImageOutput)
        }
        
        previewLayer.session = captureSession
        
        previewLayer.videoGravity = AVLayerVideoGravityResizeAspect

        let cameraPreview = UIView()
        
        if "overlay" == captureMethod {
            cameraPreview.frame = self.originalPhotoOverlay.frame
        }
        else if "comparison" == captureMethod {
            if (size.width <= size.height) {
                // Portrait
                cameraPreview.frame = CGRect(
                    x: Int(round(size.width / 2)),
                    y: 0,
                    width: Int(round(size.width / 2)),
                    height: Int(size.height - CGFloat(buttonContainerSize))
                )
            }
            else {
                // Landscape
                cameraPreview.frame = CGRect(
                    x: 0,
                    y: Int(round(size.height / 2)),
                    width: Int(size.width - CGFloat(buttonContainerSize)),
                    height: Int(round(size.height / 2))
                )
            }
        }
        
        previewLayer.frame = CGRect(
            x: 0,
            y: 0,
            width: cameraPreview.frame.width,
            height: cameraPreview.frame.height
        )

        
        let deviceOrientation = UIDevice.currentDevice().orientation
        
        if deviceOrientation == UIDeviceOrientation.LandscapeLeft {
            previewLayer.connection.videoOrientation = AVCaptureVideoOrientation.LandscapeRight
        }
        else if deviceOrientation == UIDeviceOrientation.LandscapeRight {
            previewLayer.connection.videoOrientation = AVCaptureVideoOrientation.LandscapeLeft
        }
        else if deviceOrientation == UIDeviceOrientation.PortraitUpsideDown {
            previewLayer.connection.videoOrientation = AVCaptureVideoOrientation.PortraitUpsideDown
        }
        else {
            previewLayer.connection.videoOrientation = AVCaptureVideoOrientation.Portrait
        }
        
        cameraPreview.layer.addSublayer(previewLayer)
        view.addSubview(cameraPreview)
        view.sendSubviewToBack(cameraPreview)
    }
    
    func endSession() {
        captureSession.removeInput(deviceInput)
        
        captureSession.stopRunning()
    }
    
    // MARK: Actions
    @IBAction func unwindToCapture(segue: UIStoryboardSegue) {
        
    }
    
    func takePicture(sender: UIButton!) {
        if screenshotMode {
            self.newPhoto = UIImage(named: screenshotModeOrientation + "-new.jpg")
            self.performSegueWithIdentifier("captureToConfirm", sender: self)
        }
        else {
            // Make the camera set the orientation of the image when it's taken.
            let videoOrientation = previewLayer.connection.videoOrientation
            stillImageOutput.connectionWithMediaType(AVMediaTypeVideo).videoOrientation = videoOrientation
            
            if let videoConnection = stillImageOutput.connectionWithMediaType(AVMediaTypeVideo) {
                stillImageOutput.captureStillImageAsynchronouslyFromConnection(videoConnection) {
                    (imageDataSampleBuffer, error) -> Void in
                    let imageData = AVCaptureStillImageOutput.jpegStillImageNSDataRepresentation(imageDataSampleBuffer)
                    self.newPhoto = UIImage(data: imageData)
                    self.performSegueWithIdentifier("captureToConfirm", sender: self)
                }
            }
        }
    }
    
    func switchCamera(sender: UIButton!) {
        endSession()
        cameraIndex++
        cameraIndex %= captureDevices.count
        
        // Save the last-used camera so that it defaults to this one next time.
        NSUserDefaults.standardUserDefaults().setInteger(cameraIndex, forKey: "cameraIndex")
        
        beginSession(view.frame.size)
    }
    
    func cancelCapture(sender: UIButton?) {
        self.performSegueWithIdentifier("captureToIntro", sender: self)
    }
    
    // MARK: Delegates
    
    override func buildLayout(size: CGSize) {
        super.buildLayout(size)
        
        // Add the original image overlay
        originalPhotoOverlay.image = originalPhoto
        originalPhotoOverlay.contentMode = .ScaleAspectFit
        
        if screenshotMode {
            originalPhotoOverlay.alpha = 1.0
        }
        else if "overlay" == captureMethod {
            originalPhotoOverlay.alpha = 0.85
        }
        
        if (size.width <= size.height) {
            // Portrait orientation.
            if "overlay" == captureMethod {
                originalPhotoOverlay.frame = CGRect(
                    x: 0,
                    y: 0,
                    width: size.width,
                    height: size.height - CGFloat(buttonContainerSize)
                )
            }
            else if "comparison" == captureMethod {
                originalPhotoOverlay.frame = CGRect(
                    x: 0,
                    y: 0,
                    width: Int(round(size.width / 2)),
                    height: Int(size.height - CGFloat(buttonContainerSize))
                )

            }
        }
        else {
            // Landscape orientation.
            if "overlay" == captureMethod {
                originalPhotoOverlay.frame = CGRect(
                    x: 0,
                    y: 0,
                    width: Int(Float(size.width) - Float(buttonContainerSize)),
                    height: Int(size.height)
                )
            }
            else if "comparison" == captureMethod {
                originalPhotoOverlay.frame = CGRect(
                    x: 0,
                    y: 0,
                    width: Int(Float(size.width) - Float(buttonContainerSize)),
                    height: Int(round(size.height/2))
                )
            }
        }
        
        view.addSubview(originalPhotoOverlay)
        
        // Add capture button.
        let captureButtonImage = UIImage(named: "camera-circle.png")
        captureButton.setImage(captureButtonImage, forState: .Normal)
        let captureButtonActiveImage = UIImage(named: "camera-circle-active.png")
        captureButton.setImage(captureButtonActiveImage, forState: .Highlighted)
        captureButton.setImage(captureButtonActiveImage, forState: .Selected)
        captureButton.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            
            captureButton.frame = CGRect(
                x: Int(round(size.width / 2) - round(largeButtonSize / 2)),
                y: Int(size.height - round(buttonContainerSize/2) - round(largeButtonSize/2)),
                width: Int(largeButtonSize),
                height: Int(largeButtonSize)
            )
        }
        else {
            // Landscape
            let xPos = size.width
                - round(buttonContainerSize/2)
                - round(largeButtonSize/2);
            let yPos = round(size.height / 2)
                - round(largeButtonSize / 2);
            
            captureButton.frame = CGRect(
                x: Int(xPos),
                y: Int(yPos),
                width: Int(largeButtonSize),
                height: Int(largeButtonSize)
            )
        }
        
        captureButton.addTarget(self, action:"takePicture:", forControlEvents: .TouchUpInside)
        view.addSubview(captureButton)
        
        if screenshotMode || captureDevices.count > 1 {
            // Add switch button.
            let switchCameraButtonImage = UIImage(named: "camera-switch.png")
            switchCameraButton.setImage(switchCameraButtonImage, forState: .Normal)
            switchCameraButton.contentMode = .ScaleAspectFit
            
            if (size.width < size.height) {
                // Portrait orientation.
                switchCameraButton.frame = CGRect(
                    x: Int(round(size.width / 6 * 5) - round(smallButtonSize / 2)),
                    y: Int(
                        size.height -
                            buttonContainerSize +
                            round(CGFloat(buttonContainerSize - smallButtonSize) / 2)
                    ),
                    width: Int(smallButtonSize),
                    height: Int(smallButtonSize)
                )
            }
            else {
                // Landscape
                switchCameraButton.frame = CGRect(
                    x: Int(
                        size.width -
                            buttonContainerSize +
                            round((buttonContainerSize - smallButtonSize) / 2)
                    ),
                    y: Int(round(size.height / 6 * 5) - round(smallButtonSize / 2)),
                    width: Int(smallButtonSize),
                    height: Int(smallButtonSize)
                )
            }
            
            switchCameraButton.addTarget(self, action:"switchCamera:", forControlEvents: .TouchUpInside)
            view.addSubview(switchCameraButton)
        }
        
        let cancelButtonImage = UIImage(named: "back.png")
        cancelButton.setImage(cancelButtonImage, forState: .Normal)
        cancelButton.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            cancelButton.frame = CGRect(
                x: Int(round(size.width / 6 * 1) - round(smallButtonSize / 2)),
                y: Int(
                    size.height -
                        CGFloat(buttonContainerSize) +
                        round((buttonContainerSize - smallButtonSize) / 2)
                ),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        else {
            // Landscape
            cancelButton.frame = CGRect(
                x: Int(
                    size.width -
                        buttonContainerSize +
                        round((buttonContainerSize - smallButtonSize) / 2)
                ),
                y: Int(round(size.height / 6 * 1) - round(smallButtonSize / 2)),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        
        cancelButton.addTarget(self, action:"cancelCapture:", forControlEvents: .TouchUpInside)
        view.addSubview(cancelButton)
        
        endSession()
        beginSession(size)
    }
    
    override func touchesBegan(touches: Set<UITouch>, withEvent event: UIEvent?) {
        if screenshotMode {
            self.originalPhotoOverlay.alpha = (self.originalPhotoOverlay.alpha + 0.1) % 1
            return
        }
        
        //Get Touch Point
        let Point = touches.first!.locationInView(view)

        let cameraPoint = previewLayer.captureDevicePointOfInterestForPoint(Point)
        
        //Assign Auto Focus and Auto Exposour
        if let device = captureDevice {
            do {
                try! device.lockForConfiguration()

                if device.focusPointOfInterestSupported{
                    //Add Focus on Point
                    device.focusPointOfInterest = cameraPoint
                    device.focusMode = AVCaptureFocusMode.AutoFocus
                }
                
                if device.exposurePointOfInterestSupported{
                    //Add Exposure on Point
                    device.exposurePointOfInterest = cameraPoint
                    device.exposureMode = AVCaptureExposureMode.AutoExpose
                }
                device.unlockForConfiguration()
            }
        }
    }
    
    override func swipeRight() {
        super.swipeRight()
        
        cancelCapture(nil)
    }
}

