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
    
    var originalPhoto: UIImage?
    var newPhoto: UIImage?
    var combinedPhoto: UIImage?
    
    let compareOriginal: UIImageView = UIImageView()
    let compareNew: UIImageView = UIImageView()
    let confirmButton: UIButton = UIButton()
    let cancelButton: UIButton = UIButton()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        buildLayout(view.bounds.size)
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
        UIImageWriteToSavedPhotosAlbum(newPhoto!, nil, nil, nil)
        
        // Merge the two images.
        
        let oldImageHeight = originalPhoto!.size.height
        let oldImageWidth = originalPhoto!.size.width
        
        let newImageHeight = newPhoto!.size.height
        let newImageWidth = newPhoto!.size.width
        
        var finalSize: CGSize
        var originalDest: CGRect
        var newDest: CGRect
        
        if (oldImageWidth <= oldImageHeight) {
            // Portrait:
            let smallestHeight = min(originalPhoto!.size.height, newPhoto!.size.height)
            let totalWidth = ( ( smallestHeight / oldImageHeight ) * oldImageWidth ) + ( ( smallestHeight / newImageHeight ) * newImageWidth )
            
            finalSize = CGSize(width: totalWidth, height: smallestHeight)

            originalDest = CGRect(x: 0, y: 0, width:( ( smallestHeight / oldImageHeight ) * oldImageWidth ), height: smallestHeight)
            newDest = CGRect(x: ( ( smallestHeight / oldImageHeight ) * oldImageWidth ), y: 0, width: ( ( smallestHeight / newImageHeight ) * newImageWidth ), height: smallestHeight)
        }
        else {
            // Landscape
            let smallestWidth = min(originalPhoto!.size.width, newPhoto!.size.width)
            let totalHeight = ( ( smallestWidth / oldImageWidth ) * oldImageHeight ) + ( ( smallestWidth / newImageWidth ) * newImageHeight )
            
            finalSize = CGSize(width: smallestWidth, height: totalHeight)
            
            originalDest = CGRect(x: 0, y: 0, width: smallestWidth, height: ( ( smallestWidth / oldImageWidth ) * oldImageHeight ) )
            newDest = CGRect(x: 0, y:( ( smallestWidth / oldImageWidth ) * oldImageHeight ), width: smallestWidth, height:( ( smallestWidth / newImageWidth ) * newImageHeight ) )
        }
        
        UIGraphicsBeginImageContext(finalSize)
        
        originalPhoto!.drawInRect(originalDest)
        newPhoto!.drawInRect(newDest)
        
        // Add the Reenact logo
        let logoWidth = round(finalSize.width * 0.04)
        let logoOffset = round(finalSize.width * 0.01)
        let logoDest = CGRect(x: finalSize.width - logoWidth - logoOffset, y: finalSize.height - logoWidth - logoOffset, width: logoWidth, height: logoWidth)
        let logo = UIImage(named: "logo.png")
        logo!.drawInRect(logoDest)
        
        combinedPhoto = UIGraphicsGetImageFromCurrentImageContext()
        UIGraphicsEndImageContext()

        // Save the combined image.
        UIImageWriteToSavedPhotosAlbum(combinedPhoto!, nil, nil, nil)
        
        // Send the final image off to the share controller.
        self.performSegueWithIdentifier("confirmToShare", sender: self)
    }
    
    func cancelConfirmation(sender: UIButton) {
        // Go back to the capture view.
        self.performSegueWithIdentifier("confirmToCapture", sender: self)
    }
    
    override func buildLayout(size: CGSize) {
        super.buildLayout(size)
        
        compareOriginal.image = originalPhoto
        compareOriginal.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            
            if originalPhoto!.size.width <= originalPhoto!.size.height {
                // Portrait photos
                let compareOriginalWidth = round( size.width / 2 )
                
                compareOriginal.frame = CGRect(
                    x: 0,
                    y: 0,
                    width: compareOriginalWidth,
                    height: size.height - buttonContainerSize
                )
            }
            else {
                // Landscape photos
                let compareOriginalHeight = round((size.height - CGFloat(buttonContainerSize)) / 2)
                
                compareOriginal.frame = CGRect(
                    x: 0,
                    y: 0,
                    width: size.width,
                    height: compareOriginalHeight
                )
            }
        }
        else {
            // Landscape orientation.
            if originalPhoto!.size.width <= originalPhoto!.size.height {
                // Portrait photos
                compareOriginal.frame = CGRect(
                    x: 0,
                    y: 0,
                    width: round(CGFloat(size.width - CGFloat(buttonContainerSize)) / 2),
                    height: size.height
                )
            }
            else {
                // Landscape photos
                compareOriginal.frame = CGRect(
                    x: 0,
                    y: 0,
                    width: size.width - CGFloat(buttonContainerSize),
                    height: round(size.height / 2)
                )
            }
        }
        
        view.addSubview(compareOriginal)
        
        compareNew.image = newPhoto
        compareNew.contentMode = .ScaleAspectFit
        
        if (size.width <= size.height) {
            // Portrait orientation.
            if originalPhoto!.size.width <= originalPhoto!.size.height {
                // Portrait photos
                compareNew.frame = CGRect(
                    x: round(size.width / 2),
                    y: 0,
                    width: round( size.width / 2 ),
                    height: size.height - CGFloat(buttonContainerSize)
                )
            }
            else {
                // Landscape photos
                compareNew.frame = CGRect(
                    x: 0,
                    y: round((size.height - CGFloat(buttonContainerSize)) / 2),
                    width: size.width,
                    height: round((size.height - CGFloat(buttonContainerSize)) / 2)
                )
            }

        }
        else {
            // Landscape orientation.
            if originalPhoto!.size.width <= originalPhoto!.size.height {
                // Portrait photos
                compareNew.frame = CGRect(
                    x: round(CGFloat(size.width - CGFloat(buttonContainerSize)) / 2),
                    y: 0,
                    width: round(CGFloat(size.width - CGFloat(buttonContainerSize)) / 2),
                    height: size.height
                )
            }
            else {
                compareNew.frame = CGRect(
                    x: 0,
                    y: round(size.height / 2),
                    width: size.width - CGFloat(buttonContainerSize),
                    height: round(size.height / 2)
                )
            }
        }
        
        view.addSubview(compareNew)
        
        // Add confirm button.
        let confirmButtonImage = UIImage(named: "checkmark.png")
        let confirmButtonSize = buttonContainerSize
        confirmButton.setImage(confirmButtonImage, forState: .Normal)
        confirmButton.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            confirmButton.frame = CGRect(
                x: Int(round(size.width / 2) - round(CGFloat(confirmButtonSize) / 2)),
                y: Int(size.height - CGFloat(buttonContainerSize)),
                width: confirmButtonSize,
                height: confirmButtonSize
            )
        }
        else {
            // Landscape
            confirmButton.frame = CGRect(
                x: Int(size.width - CGFloat(buttonContainerSize)),
                y: Int(round(size.height / 2) - round(CGFloat(confirmButtonSize) / 2)),
                width: confirmButtonSize,
                height: confirmButtonSize
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
                x: Int(round(size.width / 6 * 1) - round(CGFloat(smallButtonSize) / 2)),
                y: Int(
                    size.height -
                        CGFloat(buttonContainerSize) +
                        round(CGFloat(buttonContainerSize - smallButtonSize) / 2)
                ),
                width: smallButtonSize,
                height: smallButtonSize
            )
        }
        else {
            // Landscape
            cancelButton.frame = CGRect(
                x: Int(
                    size.width -
                        CGFloat(buttonContainerSize) +
                        round(CGFloat(buttonContainerSize - smallButtonSize) / 2)
                ),
                y: Int(round(size.height / 6 * 1) - round(CGFloat(smallButtonSize) / 2)),
                width: smallButtonSize,
                height: smallButtonSize
            )
        }
        
        cancelButton.addTarget(self, action:"cancelConfirmation:", forControlEvents: .TouchUpInside)
        view.addSubview(cancelButton)

    }
    
}

