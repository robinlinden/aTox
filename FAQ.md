# Frequently asked questions (FAQ):

## 1. I can't import a profile from another Tox client!

In aTox, there are still not all functions. Support for encrypted profiles
(which have a password) will be implemented in three stages:

1. Encrypted profiles cannot be imported into aTox.
2. Encrypted profiles can be imported into aTox, but they will be stored
   unencrypted. It will not affect security, as the profile is stored in the
   "system" (userdata) partition, in a separate folder where other applications
   or Android operating system do not have access, only aTox itself. Exceptions
   are only applications with root access.
3. Encrypted profiles can be imported into aTox, and they will always be stored
   fully encrypted.

aTox 0.6.0 and earlier versions are at the first stage. To import a profile
from another Tox client to aTox, you must first remove the password from the
profile if your client supports this feature. aTox 0.7.0 moved to the second
stage. When, and if the transition to step three will be produced - this note
will be updated.


## 2. How much aTox consumes the battery charge and Internet traffic?

Here are average values based on the results of several people using only TCP
protocol (standard network settings). Switching on of the UDP protocol
significantly increases consumption of the battery charge and Internet traffic,
and there are no statistics with its use at the moment, it may be added later.
Results may vary, depending on the frequency of sending messages and files, the
weight of files, as well as the frequency and duration of using audio calls and
audio messages:

0.5-0.6% battery charge per hour, or 12-14.4% in 24 hours.
2.6 megabyte Internet traffic per hour, or 62.4 megabytes for 24 hours, or
1934.4 megabytes (1.93 gigabytes) for the month (31 days).


## 3. How do I not lose my profile?

Your profile stores all your contacts and messages. Because the Tox protocol
does not use central servers, your profile is stored only on your device. This
means that only you are responsible for its safety. If the only one a copy of
the profile will be stored only on your smartphone with installed aTox
(although it applies to any platform and any Tox client), any of the listed
situations may happen to you, because of which you temporarily, or permanently
lose your profile:

1. You can remove aTox.
2. You can make a reset of the entire operating system, which will delete all
   your data.
3. You can break your smartphone, or it will break itself.
4. You can lose your smartphone.
5. Your smartphone can be stolen.

And this is not a complete list of possible situations. Moreover, in a similar
way, you can lose any important data, and not just Tox profile. To protect
yourself, you need to make backup copies of important data. Never keep them in
a single copy on one device or data storage. In the case of aTox, you can make
a backup of the profile as follows:

On the screen with the contact list, press the button in the upper left corner
(three horizontal strips), or swipe from left to right. In the opened panel,
click on the "Export Tox profile" button, then select the folder to which you
want to copy profile, and click on the "Save" button.

Done. It remains to copy this file from your smartphone to somewhere else for
keeping, for example, on your computer, USB flash drive, etc. Now your profile
are in safe. Do not forget to periodically update your profile copies, so that
they are always relevant.


## 4. Can I use one profile on multiple devices at the same time?

No, the Tox protocol does not support the simultaneous use of one profile on
multiple devices. This can be done only in turn. For example, be in the Tox
network from one device while the other is disabled from it. And vice versa. If
you are try to do this, your connection will be unstable, messages may not be
sent/received, and many other problems will occur.
