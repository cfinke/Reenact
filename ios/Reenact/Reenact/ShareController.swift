//
//  ShareController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class ShareController: ReenactControllerBase {
    // MARK: Properties
    
    var combinedPhoto: UIImage!
    
    let comparisonImage: UIImageView = UIImageView()
    let shareButton: UIButton = UIButton()
    let restartButton: UIButton = UIButton()
    let saveNotification: UIButton = UIButton()
    
    override func viewDidLoad() {
        super.viewDidLoad()

        buildLayout(view.bounds.size)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Actions
    func startOver(sender: UIButton!){
        self.performSegueWithIdentifier("shareToIntro", sender: self)
    }
    
    func share(sender: UIButton!) {
        let objectsToShare = [combinedPhoto]
        let activityVC = UIActivityViewController(activityItems: objectsToShare, applicationActivities: nil)
        activityVC.popoverPresentationController?.sourceView = sender
        self.presentViewController(activityVC, animated: true, completion: nil)
    }
    
    // MARK: Delegates
    
    override func buildLayout(size: CGSize) {
        super.buildLayout(size)
        
        // Load the comparison image.
        comparisonImage.image = combinedPhoto
        comparisonImage.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            comparisonImage.frame = CGRect(
                x: 0,
                y: 0,
                width: Int(size.width),
                height: Int(size.height - CGFloat(buttonContainerSize))
            )
        }
        else {
            // Landscape orientation.
            comparisonImage.frame = CGRect(
                x: 0,
                y: 0,
                width: Int(size.width - CGFloat(buttonContainerSize)),
                height: Int(size.height)
            )
        }
        
        view.addSubview(comparisonImage)
        
        // Add share button
        

        
        let shareButtonImage = UIImage(named: "share.png")
        shareButton.setImage(shareButtonImage, forState: .Normal)
        shareButton.contentMode = .ScaleAspectFit
        
        
        
        if (size.width < size.height) {
            // Portrait orientation.
            shareButton.frame = CGRect(
                x: Int(round(size.width / 2) - round(smallButtonSize / 2)),
                y: Int(
                    size.height
                        - round(buttonContainerSize / 2)
                        - round(smallButtonSize / 2)
                ),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        else {
            // Landscape
            let xPos = size.width
                - round(buttonContainerSize / 2)
                - round(smallButtonSize / 2);
            let yPos = round(size.height / 2) - round(smallButtonSize / 2);
            
            shareButton.frame = CGRect(
                x: Int(xPos),
                y: Int(yPos),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        
        shareButton.addTarget(self, action:"share:", forControlEvents: .TouchUpInside)
        view.addSubview(shareButton)
        
        // Add restart button.
        let restartButtonImage = UIImage(named: "replay.png")
        restartButton.setImage(restartButtonImage, forState: .Normal)
        restartButton.contentMode = .ScaleAspectFit
        
        if (size.width < size.height) {
            // Portrait orientation.
            restartButton.frame = CGRect(
                x: Int(round(size.width / 6 * 5) - round(smallButtonSize / 2)),
                y: Int(
                    size.height
                        - round(buttonContainerSize / 2)
                        - round(smallButtonSize / 2)
                ),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        else {
            // Landscape
            let xPos = size.width
                    - round(buttonContainerSize / 2)
                    - round(smallButtonSize / 2);
            let yPos = round(size.height / 6 * 5) - round(smallButtonSize / 2);
            
            restartButton.frame = CGRect(
                x: Int(xPos),
                y: Int(yPos),
                width: Int(smallButtonSize),
                height: Int(smallButtonSize)
            )
        }
        
        restartButton.addTarget(self, action:"startOver:", forControlEvents: .TouchUpInside)
        view.addSubview(restartButton)
        
        let saveNotificationFont = UIFont(name:"Helvetica Neue", size: 14)
        var saveNotificationSize = textSize("Photo Saved", font: saveNotificationFont!)
        let saveNotificationPadding: CGFloat = 10.0
        
        saveNotification.setTitle("Photo Saved", forState: .Normal)
        saveNotification.setTitleColor(UIColor.whiteColor(), forState: .Normal)
        saveNotification.backgroundColor = UIColor.blackColor()
        saveNotification.contentEdgeInsets = UIEdgeInsetsMake(saveNotificationPadding, saveNotificationPadding, saveNotificationPadding, saveNotificationPadding)
        saveNotification.titleLabel!.font = saveNotificationFont
        saveNotificationSize.width *= 1.5
        
        if (size.width <= size.height) {
            saveNotification.frame = CGRect(
                x: Int(round((size.width - saveNotificationSize.width) / 2) - saveNotificationPadding),
                y: Int(saveNotificationSize.height),
                width: Int((saveNotificationSize.width) + (2 * saveNotificationPadding)),
                height: Int(saveNotificationSize.height + (2 * saveNotificationPadding))
            )
        }
        else {
            saveNotification.frame = CGRect(
                x: Int(((size.width - buttonContainerSize - saveNotificationSize.width) / 2) - saveNotificationPadding),
                y: Int(saveNotificationSize.height),
                width: Int((saveNotificationSize.width) + (2 * saveNotificationPadding)),
                height: Int(saveNotificationSize.height + (2 * saveNotificationPadding))
            )
        }
        view.addSubview(saveNotification)
        
        UIView.animateWithDuration( 3.0, delay: 2.0, options: UIViewAnimationOptions.CurveEaseOut, animations : {
            self.saveNotification.alpha = 0;
            }, completion : {
                (finished: Bool) -> Void in
            }
        )
        
    }
}

