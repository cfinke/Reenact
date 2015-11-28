//
//  ConfirmController.swift
//  Reenact
//
//  Created by Christopher Finke on 11/25/15.
//  Copyright © 2015 Christopher Finke. All rights reserved.
//

import UIKit

class ConfirmController: UIViewController {
    // MARK: Properties
    
    @IBOutlet weak var compareOriginal: UIImageView!
    @IBOutlet weak var compareNew: UIImageView!
    
    var originalPhoto: UIImage?
    var newPhoto: UIImage?
    
    override func viewDidLoad() {
        super.viewDidLoad()
        // Do any additional setup after loading the view, typically from a nib.
        print("In confirm")
        print(originalPhoto)
        print(newPhoto)
        
        compareOriginal.image = originalPhoto
        compareNew.image = newPhoto
    }
    
    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject!) {
        if (segue.identifier == "confirmToShare") {
            print("In prepareForSegue")
            
//            let svc = segue.destinationViewController as! ShareController;
        }
    }
    
    
    // MARK: Actions
    
    // MARK: Delegates
    
}

