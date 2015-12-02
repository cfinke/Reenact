//
//  ShareController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class HelpController: ReenactControllerBase {
    // MARK: Properties
    
    let closeButton: UIButton = UIButton()
    let reenactLogoView: UIImageView = UIImageView()
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        buildLayout(view.bounds.size)
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    // MARK: Actions
    func closeHelp(sender: UIButton?) {
        self.performSegueWithIdentifier("helpToIntro", sender: self)
    }
    
    override func buildLayout(size: CGSize) {
        super.buildLayout(size)
        
        let reenactLogo = UIImage(named:"logo.png")
        reenactLogoView.image = reenactLogo
        reenactLogoView.contentMode = .ScaleAspectFit

        let instructions: [String] = ["Reenact photos with Reenact. Choose a photo, align the camera, and take the shot.", "For help, email help@reenact.me", "@ReenactApp on Twitter", "http://reenact.me/"]
        let instructionsFont = UIFont(name:"Helvetica Neue", size: 16.0)
        let textMargin = 30
        let lineHeight = textSize("x", font: instructionsFont!).height

        let reenactLogoWidth = Int(round(min(size.height, size.width) / 2))

        if (size.width <= size.height) {
            // Add the logo on the intro screen.
            reenactLogoView.frame = CGRect(
                x: Int(round(size.width / 2) - round(CGFloat(reenactLogoWidth) / 2)),
                y: Int(round(size.height / 4) - round(CGFloat(reenactLogoWidth) / 2)),
                width: reenactLogoWidth,
                height: reenactLogoWidth
            )

            let maxTextWidth = Int(size.width) - (textMargin * 2)
            var currentYOffset = CGFloat(Int(round(size.height / 2)))
            
            for instruction in instructions {
                var instructionSize = textSize(instruction, font: instructionsFont!)
                
                let numLines = ceil((instructionSize.width * 1.1) / CGFloat(maxTextWidth))
                instructionSize.height = instructionSize.height * numLines * 1.5 // 1.5 is for line spacing
                instructionSize.width = CGFloat(maxTextWidth)
                
                let instructionView = UITextView()
                instructionView.text = instruction
                instructionView.textColor = UIColor.whiteColor()
                instructionView.backgroundColor = UIColor.blackColor()
                instructionView.font = instructionsFont
                instructionView.dataDetectorTypes = .Link
                instructionView.editable = false
                instructionView.selectable = true
                
                instructionView.frame = CGRect(
                    x: textMargin,
                    y: Int(round(currentYOffset)),
                    width: maxTextWidth,
                    height: Int(instructionSize.height)
                )
                
                view.addSubview(instructionView)
                
                currentYOffset += instructionSize.height + CGFloat(lineHeight)
            }
            
        }
        else {
            reenactLogoView.frame = CGRect(
                x: Int(round((size.width / 4) - (CGFloat(reenactLogoWidth) / 2))),
                y: Int(round(size.height / 4)),
                width: reenactLogoWidth,
                height: reenactLogoWidth
            )
            
            let maxTextWidth = Int(size.width / 2) - (textMargin * 2)
            var currentYOffset = CGFloat(textMargin)
            
            for instruction in instructions {
                var instructionSize = textSize(instruction, font: instructionsFont!)

                let numLines = ceil((instructionSize.width * 1.1) / CGFloat(maxTextWidth))
                instructionSize.height = instructionSize.height * numLines * 1.5 // 1.5 is for line spacing
                instructionSize.width = CGFloat(maxTextWidth)
                
                let instructionView = UITextView()
                instructionView.text = instruction
                instructionView.textColor = UIColor.whiteColor()
                instructionView.backgroundColor = UIColor.blackColor()
                instructionView.font = instructionsFont
                instructionView.dataDetectorTypes = .Link
                instructionView.editable = false
                instructionView.selectable = true
                
                instructionView.frame = CGRect(
                    x: Int(round(size.width / 2)) + textMargin,
                    y: Int(round(currentYOffset)),
                    width: Int(instructionSize.width),
                    height: Int(instructionSize.height)
                )
                
                view.addSubview(instructionView)
                
                currentYOffset += instructionSize.height + CGFloat(lineHeight)
            }
        }
        
        view.addSubview(reenactLogoView)
        
        let closeButtonImage = UIImage(named:"close.png")
        let closeButtonSize = 30
        let closeButtonOffset = 30
        closeButton.setImage(closeButtonImage, forState: .Normal)
        closeButton.frame = CGRect(
            x: Int(size.width - CGFloat(closeButtonOffset) - CGFloat(closeButtonSize)),
            y: closeButtonOffset,
            width: closeButtonSize,
            height: closeButtonSize
        )
        closeButton.contentMode = .ScaleAspectFit
        closeButton.addTarget(self, action:"closeHelp:", forControlEvents: .TouchUpInside)
        view.addSubview(closeButton)
    }
    
    
    override func swipeRight() {
        super.swipeRight()
        
        closeHelp(nil)
    }
}

