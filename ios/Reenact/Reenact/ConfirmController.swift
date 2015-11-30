//
//  ConfirmController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class ConfirmController: ReenactControllerBase {
    // MARK: Properties
    
    var originalPhoto: UIImage!
    var newPhoto: UIImage!
    var combinedPhoto: UIImage!
    
    let confirmButton: UIButton = UIButton()
    let cancelButton: UIButton = UIButton()
    let comparisonImage: UIImageView = UIImageView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        buildLayout(view.bounds.size)
    }
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        combinedPhoto = buildComparison(originalPhoto, photo2: newPhoto)
        comparisonImage.contentMode = .ScaleAspectFit
        comparisonImage.image = combinedPhoto
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject!) {
        print("In prepareForSegue")
        
        if (segue.identifier == "confirmToShare") {
            let svc = segue.destinationViewController as! ShareController;
            svc.combinedPhoto = combinedPhoto
        }
    }
    
    
    // MARK: Actions
    
    func confirmShot(sender: UIButton) {
        // Save the new shot separately.
        UIImageWriteToSavedPhotosAlbum(newPhoto, nil, nil, nil)

        // Save the combined image.
        UIImageWriteToSavedPhotosAlbum(combinedPhoto, nil, nil, nil)
        
        // Send the final image off to the share controller.
        self.performSegueWithIdentifier("confirmToShare", sender: self)
    }
    
    func cancelConfirmation(sender: UIButton) {
        // Go back to the capture view.
        self.performSegueWithIdentifier("confirmToCapture", sender: self)
    }
    
    override func buildLayout(size: CGSize) {
        super.buildLayout(size)
        
        if (size.width < size.height) {
            // Portrait orientation.
            comparisonImage.frame = CGRect(
                x: 0,
                y: 0,
                width: size.width,
                height: size.height - buttonContainerSize
            )
        }
        else {
            // Landscape orientation.
            comparisonImage.frame = CGRect(
                x: 0,
                y: 0,
                width: size.width - buttonContainerSize,
                height: size.height
            )
        }
        
        view.addSubview(comparisonImage)
        
        // Add confirm button.
        let confirmButtonImage = UIImage(named: "checkmark.png")
        confirmButton.setImage(confirmButtonImage, forState: .Normal)
        confirmButton.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            confirmButton.frame = CGRect(
                x: Int(round(size.width / 2) - round(buttonContainerSize / 2)),
                y: Int(size.height - buttonContainerSize),
                width: Int(buttonContainerSize),
                height: Int(buttonContainerSize)
            )
        }
        else {
            // Landscape
            confirmButton.frame = CGRect(
                x: Int(size.width - buttonContainerSize),
                y: Int(round(size.height / 2) - round(buttonContainerSize / 2)),
                width: Int(buttonContainerSize),
                height: Int(buttonContainerSize)
            )
        }
        
        confirmButton.addTarget(self, action:"confirmShot:", forControlEvents: .TouchUpInside)
        view.addSubview(confirmButton)
        
        // Add cancel button.
        let cancelButtonImage = UIImage(named: "back.png")
        cancelButton.setImage(cancelButtonImage, forState: .Normal)
        cancelButton.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            cancelButton.frame = CGRect(
                x: Int(round(size.width / 6 * 1) - round(smallButtonSize / 2)),
                y: Int(
                    size.height -
                        buttonContainerSize +
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
        
        cancelButton.addTarget(self, action:"cancelConfirmation:", forControlEvents: .TouchUpInside)
        view.addSubview(cancelButton)

    }
    
    func buildComparison(photo1: UIImage, photo2: UIImage) -> UIImage {
        let oldImageHeight = photo1.size.height
        let oldImageWidth = photo1.size.width
        
        let newImageHeight = photo2.size.height
        let newImageWidth = photo2.size.width
        
        var finalSize: CGSize
        var originalDest: CGRect
        var newDest: CGRect
        
        if (oldImageWidth <= oldImageHeight) {
            // Portrait:
            let smallestHeight = min(photo1.size.height, photo2.size.height)
            let totalWidth = ( ( smallestHeight / oldImageHeight ) * oldImageWidth ) + ( ( smallestHeight / newImageHeight ) * newImageWidth )
            
            finalSize = CGSize(width: totalWidth, height: smallestHeight)
            
            originalDest = CGRect(x: 0, y: 0, width:( ( smallestHeight / oldImageHeight ) * oldImageWidth ), height: smallestHeight)
            newDest = CGRect(x: ( ( smallestHeight / oldImageHeight ) * oldImageWidth ), y: 0, width: ( ( smallestHeight / newImageHeight ) * newImageWidth ), height: smallestHeight)
        }
        else {
            // Landscape
            let smallestWidth = min(photo1.size.width, photo2.size.width)
            let totalHeight = ( ( smallestWidth / oldImageWidth ) * oldImageHeight ) + ( ( smallestWidth / newImageWidth ) * newImageHeight )
            
            finalSize = CGSize(width: smallestWidth, height: totalHeight)
            
            originalDest = CGRect(x: 0, y: 0, width: smallestWidth, height: ( ( smallestWidth / oldImageWidth ) * oldImageHeight ) )
            newDest = CGRect(x: 0, y:( ( smallestWidth / oldImageWidth ) * oldImageHeight ), width: smallestWidth, height:( ( smallestWidth / newImageWidth ) * newImageHeight ) )
        }
        
        UIGraphicsBeginImageContext(finalSize)
        
        photo1.drawInRect(originalDest)
        photo2.drawInRect(newDest)
        
        // Add the Reenact logo
        let logoWidth = round(finalSize.width * 0.04)
        let logoOffset = round(finalSize.width * 0.01)
        let logoDest = CGRect(x: finalSize.width - logoWidth - logoOffset, y: finalSize.height - logoWidth - logoOffset, width: logoWidth, height: logoWidth)
        let logo = UIImage(named: "logo.png")
        logo?.drawInRect(logoDest)
        
        combinedPhoto = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        return combinedPhoto!
    }
    
}

